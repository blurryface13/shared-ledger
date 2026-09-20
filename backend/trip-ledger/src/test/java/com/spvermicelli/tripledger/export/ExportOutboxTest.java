package com.spvermicelli.tripledger.export;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.spvermicelli.tripledger.export.infrastructure.messaging.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.core.io.ClassPathResource;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

class ExportOutboxTest {
    JdbcTemplate jdbc;
    DataSourceTransactionManager manager;
    ExportTaskPublisher publisher;
    ExportOutboxRelay relay;
    ExportOutbox outbox;
    @BeforeEach void setup() throws Exception {
        String url=System.getenv("TRIP_LEDGER_IDEMPOTENCY_TEST_DB_URL");
        assertNotNull(url);assertTrue(url.contains("/trip_ledger_idempotency_test?"));
        var ds=new DriverManagerDataSource(url,System.getenv("TRIP_LEDGER_TEST_DB_USERNAME"),System.getenv("TRIP_LEDGER_TEST_DB_PASSWORD"));
        jdbc=new JdbcTemplate(ds);manager=new DataSourceTransactionManager(ds);
        jdbc.execute(new ClassPathResource("export-outbox-schema.sql").getContentAsString(StandardCharsets.UTF_8));
        jdbc.update("DELETE FROM export_outbox");
        publisher=mock(ExportTaskPublisher.class);relay=new ExportOutboxRelay(jdbc,publisher,manager);outbox=new ExportOutbox(jdbc);
    }
    void enqueue(long id) { new TransactionTemplate(manager).executeWithoutResult(s->outbox.enqueue(id)); }
    String state(long id) {return jdbc.queryForObject("SELECT status FROM export_outbox WHERE export_record_id=?",String.class,id);}
    @Test void taskRollbackAlsoRemovesOutbox() {
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(manager).executeWithoutResult(s->{outbox.enqueue(1L);throw new IllegalStateException();}));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM export_outbox",Integer.class));
    }
    @Test void committedRecordSurvivesRelayRecreation() {
        enqueue(1);var restarted=new ExportOutboxRelay(jdbc,publisher,manager);
        assertTrue(restarted.relayOne());assertEquals("SENT",state(1));assertFalse(restarted.relayOne());verify(publisher,times(1)).publish(1L);
    }
    @Test void brokerFailureBacksOffAndEventuallySucceeds() {
        enqueue(1);doThrow(new IllegalStateException("offline")).doNothing().when(publisher).publish(1L);
        relay.relayOne();assertEquals("PENDING",state(1));assertFalse(relay.relayOne());
        jdbc.update("UPDATE export_outbox SET next_attempt_at=CURRENT_TIMESTAMP(6)");relay.relayOne();
        assertEquals("SENT",state(1));assertEquals(2,jdbc.queryForObject("SELECT attempts FROM export_outbox",Integer.class));
    }
    @Test void permanentFailureStopsAtEightAttempts() {
        enqueue(1);doThrow(new IllegalStateException()).when(publisher).publish(1L);
        for(int i=0;i<8;i++){jdbc.update("UPDATE export_outbox SET next_attempt_at=CURRENT_TIMESTAMP(6)");assertTrue(relay.relayOne());}
        assertEquals("DEAD",state(1));assertFalse(relay.relayOne());verify(publisher,times(8)).publish(1L);
    }
    @Test void concurrentRelaysSkipLockedRecord() throws Exception {
        enqueue(1);enqueue(2);var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        doAnswer(inv->{entered.countDown();assertTrue(release.await(5,TimeUnit.SECONDS));return null;}).when(publisher).publish(1L);
        try(var pool=Executors.newSingleThreadExecutor()) {
            var first=pool.submit(()->relay.relayOne());
            try {assertTrue(entered.await(5,TimeUnit.SECONDS));assertTrue(new ExportOutboxRelay(jdbc,publisher,manager).relayOne());assertEquals("SENT",state(2));}
            finally {release.countDown();}
            assertTrue(first.get(5,TimeUnit.SECONDS));
        }
        verify(publisher,times(1)).publish(1L);verify(publisher,times(1)).publish(2L);
    }
    @Test void databaseRollbackAfterBrokerAckAllowsDuplicateDelivery() {
        enqueue(1);
        // Simulate process failure after publish by wrapping the relay transaction and rolling it back.
        doNothing().when(publisher).publish(1L);
        new TransactionTemplate(manager).executeWithoutResult(s->{relay.relayOne();s.setRollbackOnly();});
        assertEquals("PENDING",state(1));relay.relayOne();verify(publisher,times(2)).publish(1L);
    }
}
