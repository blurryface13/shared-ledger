package com.spvermicelli.tripledger.shared.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.core.io.ClassPathResource;

class DurableIdempotencyServiceTest {
    JdbcTemplate jdbc;
    DurableIdempotencyService service;
    static final String KEY = "test-request-000001";
    @BeforeEach void setup() throws Exception {
        String url = System.getenv("TRIP_LEDGER_IDEMPOTENCY_TEST_DB_URL");
        assertNotNull(url, "Explicit isolated test database required");
        assertTrue(url.contains("/trip_ledger_idempotency_test?"), "Use dedicated trip_ledger_idempotency_test database");
        var ds = new DriverManagerDataSource(url, System.getenv("TRIP_LEDGER_TEST_DB_USERNAME"), System.getenv("TRIP_LEDGER_TEST_DB_PASSWORD"));
        jdbc = new JdbcTemplate(ds);
        jdbc.execute(new ClassPathResource("idempotency-schema.sql").getContentAsString(StandardCharsets.UTF_8));
        jdbc.execute("CREATE TABLE IF NOT EXISTS idempotency_test_effect(id BIGINT PRIMARY KEY, amount INT NOT NULL)");
        jdbc.update("DELETE FROM request_idempotency WHERE operation='test'");
        jdbc.update("DELETE FROM idempotency_test_effect");
        service = new DurableIdempotencyService(jdbc,new ObjectMapper(),new DataSourceTransactionManager(ds));
    }
    String run(String key, Object body, java.util.function.Supplier<String> action) {
        return service.execute(1L,1L,"test",key,body,String.class,()->{},action);
    }
    @Test void replaySurvivesNewServiceInstance() {
        assertEquals("saved",run(KEY,Map.of("amount",12),()->{
            jdbc.update("INSERT INTO idempotency_test_effect VALUES(1,12)");return "saved";
        }));
        var replacement = new DurableIdempotencyService(jdbc,new ObjectMapper(),new DataSourceTransactionManager(jdbc.getDataSource()));
        assertEquals("saved",replacement.execute(1L,1L,"test",KEY,Map.of("amount",12),String.class,()->{},()->{throw new AssertionError("executed twice");}));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM idempotency_test_effect",Integer.class));
    }
    @Test void changedPayloadRejected() {
        run(KEY,Map.of("amount",12),()->"saved");
        assertThrows(BusinessException.class,()->run(KEY,Map.of("amount",13),()->"wrong"));
    }
    @Test void failureRollsBackBothTablesAndAllowsRetry() {
        assertThrows(IllegalStateException.class,()->run(KEY,Map.of(),()->{
            jdbc.update("INSERT INTO idempotency_test_effect VALUES(1,12)");throw new IllegalStateException("rollback");
        }));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM idempotency_test_effect",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM request_idempotency WHERE operation='test'",Integer.class));
        assertEquals("retry",run(KEY,Map.of(),()->"retry"));
    }
    @Test void concurrentSameKeyExecutesOnlyOnce() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        try(var pool=Executors.newFixedThreadPool(6)) {
            var gate = new CountDownLatch(1);
            var futures = new java.util.ArrayList<Future<String>>();
            for(int i=0;i<6;i++) futures.add(pool.submit(()->{
                gate.await();return run(KEY,Map.of("amount",12),()->{calls.incrementAndGet();return "saved";});
            }));
            gate.countDown();
            for(var f:futures) assertEquals("saved",f.get(20,TimeUnit.SECONDS));
        }
        assertEquals(1,calls.get());
    }
    @Test void replayChecksCurrentPermission() {
        run(KEY,Map.of(),()->"saved");
        assertThrows(SecurityException.class,()->service.execute(1L,1L,"test",KEY,Map.of(),String.class,
            ()->{throw new SecurityException("revoked");},()->"wrong"));
    }
    @Test void objectKeyOrderDoesNotChangeFingerprint() {
        var first=new java.util.LinkedHashMap<String,Integer>();first.put("b",2);first.put("a",1);
        var second=new java.util.LinkedHashMap<String,Integer>();second.put("a",1);second.put("b",2);
        run(KEY,first,()->"saved");assertEquals("saved",run(KEY,second,()->"wrong"));
    }
    @Test void differentUserHasIndependentScope() {
        run(KEY,Map.of(),()->"first");
        assertEquals("second",service.execute(2L,1L,"test",KEY,Map.of(),String.class,()->{},()->"second"));
    }
    @Test void billResultCanBeReplayed() {
        var original = com.spvermicelli.tripledger.billing.application.bill.result.BillOperationResult.builder()
            .billId(42L).message("created").build();
        service.execute(1L,1L,"test",KEY,Map.of(),com.spvermicelli.tripledger.billing.application.bill.result.BillOperationResult.class,()->{},()->original);
        var replay = service.execute(1L,1L,"test",KEY,Map.of(),com.spvermicelli.tripledger.billing.application.bill.result.BillOperationResult.class,()->{},()->{throw new AssertionError();});
        assertEquals(42L,replay.getBillId());
        assertEquals("created",replay.getMessage());
    }
    @Test void invalidKeyRejectedBeforeWriting() {
        assertThrows(BusinessException.class,()->run("short",Map.of(),()->"wrong"));
    }
}
