package com.spvermicelli.tripledger.travel;

import com.spvermicelli.tripledger.travel.domain.MapGateway;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties="app.auth.enabled=true")
class TravelIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean MapGateway maps;
    final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
    final List<Long> users=new ArrayList<>(), tripIds=new ArrayList<>();
    String login() throws Exception {
        String code="travel-test-"+UUID.randomUUID();
        jdbc.update("INSERT INTO tb_user(wechat_open_id,nickname,mobile,status) VALUES(?,?,?,'ACTIVE')","mock-openid-"+code,"旅行测试","199"+String.format("%08d",Math.abs(code.hashCode()%100000000)));
        users.add(jdbc.queryForObject("SELECT id FROM tb_user WHERE wechat_open_id=?",Long.class,"mock-openid-"+code));
        var result=call(post("/api/v1/auth/wechat-login").content(json.writeValueAsString(Map.of("code",code))),null);
        assertEquals(0,result.path("code").asInt(),result.toString());return result.path("data").path("token").asText();
    }
    JsonNode call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,String token)throws Exception{
        if(token!=null)request.header("Authorization","Bearer "+token);
        return json.readTree(mvc.perform(request.contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse().getContentAsString());
    }
    @AfterEach void cleanup(){for(Long id:tripIds){jdbc.update("DELETE FROM tb_trip_invite WHERE trip_id=?",id);jdbc.update("DELETE FROM tb_trip_change WHERE trip_id=?",id);jdbc.update("DELETE FROM tb_trip_member WHERE trip_id=?",id);jdbc.update("DELETE FROM tb_trip_plan WHERE trip_id=?",id);jdbc.update("DELETE FROM tb_trip WHERE id=?",id);}for(Long id:users){jdbc.update("DELETE FROM tb_user_refresh_token WHERE user_id=?",id);jdbc.update("DELETE FROM tb_user WHERE id=?",id);}}
    @Autowired com.spvermicelli.tripledger.travel.application.TripApplicationService trips;
    @Test void sharedRolesRevocationAuditAndConcurrentVersionConflict() throws Exception {
        String owner=login(), member=login();long ownerId=users.get(0),memberId=users.get(1);
        String payload="""
            {"name":"共享杭州","destination":"杭州","startDate":"2026-09-26","endDate":"2026-09-27","people":2,"budgetCent":400000,"pace":"balanced","style":"culture","stay":"metro","activities":[]}
            """;
        var created=call(post("/api/v1/trips").content(payload),owner);assertEquals(0,created.path("code").asInt());
        long id=created.path("data").path("id").asLong();tripIds.add(id);
        String path="/api/v1/trips/"+id;
        assertEquals(0,call(put(path+"/members/"+memberId).content("{\"role\":\"VIEWER\"}"),owner).path("code").asInt());
        assertEquals(0,call(get(path),member).path("code").asInt());
        assertEquals(4003,call(put(path).content(created.path("data").toString()),member).path("code").asInt());
        assertEquals(4003,call(put(path+"/members/"+ownerId).content("{\"role\":\"EDITOR\"}"),member).path("code").asInt());
        assertEquals(0,call(put(path+"/members/"+memberId).content("{\"role\":\"EDITOR\"}"),owner).path("code").asInt());
        var rebound=(com.fasterxml.jackson.databind.node.ObjectNode)created.path("data").deepCopy();
        rebound.put("bookId",999999L);
        assertEquals(4003,call(put(path).content(rebound.toString()),member).path("code").asInt());
        assertEquals(1,call(get("/api/v1/trips"),member).path("data").size());
        var before=trips.get(ownerId,id);
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        var gate=new java.util.concurrent.CountDownLatch(1);
        java.util.List<java.util.concurrent.Future<Boolean>> results=new ArrayList<>();
        try {
            for(long actor:new long[]{ownerId,memberId}) results.add(pool.submit(()->{
                gate.await();try{trips.update(actor,id,before);return true;}
                catch(com.spvermicelli.tripledger.shared.common.exception.BusinessException conflict){
                    assertEquals(com.spvermicelli.tripledger.shared.common.enums.ErrorCode.CONFLICT,conflict.getErrorCode());return false;
                }
            }));
            gate.countDown();int successes=0;for(var result:results)if(result.get(10,java.util.concurrent.TimeUnit.SECONDS))successes++;
            assertEquals(1,successes);
        } finally {pool.shutdownNow();}
        assertEquals(1,trips.get(ownerId,id).version());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM tb_trip_change WHERE trip_id=? AND action='UPDATED'",Integer.class,id));
        assertEquals(0,call(get(path+"/changes"),member).path("code").asInt());
        assertEquals(0,call(delete(path+"/members/"+memberId),owner).path("code").asInt());
        assertEquals(4004,call(get(path),member).path("code").asInt());
        assertEquals(4004,call(get(path+"/changes"),member).path("code").asInt());
        assertEquals(4003,call(put(path).content(created.path("data").toString()),member).path("code").asInt());
    }

    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Test void rollbackRemovesBothTripMutationAndAudit() throws Exception {
        login();long owner=users.getFirst();
        var base=new com.spvermicelli.tripledger.travel.domain.Trip(null,0,"事务回滚","杭州",
            java.time.LocalDate.of(2026,9,26),java.time.LocalDate.of(2026,9,27),2,10000,"balanced","culture","metro",null,false,List.of());
        var trip=trips.create(owner,base);tripIds.add(trip.id());
        var tx=new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        tx.execute(status->{trips.update(owner,trip.id(),trip);status.setRollbackOnly();return null;});
        assertEquals(0,trips.get(owner,trip.id()).version());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM tb_trip_change WHERE trip_id=? AND action='UPDATED'",Integer.class,trip.id()));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM tb_trip_change WHERE trip_id=? AND action='CREATED'",Integer.class,trip.id()));
    }

    @Autowired com.spvermicelli.tripledger.travel.application.TripInvitationService invitations;
    @Autowired com.spvermicelli.tripledger.travel.application.TripCollaborationService collaboration;
    @Test void invitationRequiresRecipientConsentAndCannotRestoreRevokedMembership() throws Exception {
        login();login();login();long owner=users.get(0),recipient=users.get(1),other=users.get(2);
        var trip=trips.create(owner,new com.spvermicelli.tripledger.travel.domain.Trip(null,0,"邀请","杭州",java.time.LocalDate.of(2026,9,26),java.time.LocalDate.of(2026,9,27),2,10000,"balanced","culture","metro",null,false,List.of()));tripIds.add(trip.id());
        var invite=invitations.create(owner,trip.id(),recipient,"EDITOR");
        assertThrows(com.spvermicelli.tripledger.shared.common.exception.BusinessException.class,()->trips.get(recipient,trip.id()));
        assertThrows(com.spvermicelli.tripledger.shared.common.exception.BusinessException.class,()->invitations.accept(other,invite.token()));
        assertEquals(trip.id().longValue(),invitations.accept(recipient,invite.token()));
        assertEquals(trip.id().longValue(),invitations.accept(recipient,invite.token()));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM tb_trip_change WHERE trip_id=? AND action='INVITE_ACCEPTED'",Integer.class,trip.id()));
        jdbc.update("DELETE FROM tb_trip_member WHERE trip_id=? AND user_id=?",trip.id(),recipient);
        assertThrows(com.spvermicelli.tripledger.shared.common.exception.BusinessException.class,()->invitations.accept(recipient,invite.token()));
        var revoked=invitations.create(owner,trip.id(),recipient,"VIEWER");invitations.revoke(owner,trip.id(),revoked.id());
        assertThrows(com.spvermicelli.tripledger.shared.common.exception.BusinessException.class,()->invitations.accept(recipient,revoked.token()));
        var removed=invitations.create(owner,trip.id(),recipient,"VIEWER");
        collaboration.removeMember(owner,trip.id(),recipient);
        assertThrows(com.spvermicelli.tripledger.shared.common.exception.BusinessException.class,()->invitations.accept(recipient,removed.token()));
        var expired=invitations.create(owner,trip.id(),recipient,"VIEWER");jdbc.update("UPDATE tb_trip_invite SET expires_at=DATE_SUB(NOW(),INTERVAL 1 SECOND) WHERE id=?",expired.id());
        assertThrows(com.spvermicelli.tripledger.shared.common.exception.BusinessException.class,()->invitations.accept(recipient,expired.token()));
    }

    @Test void invitationHistoryIsOwnerOnlyPaginatedAndRedacted() throws Exception {
        String ownerToken=login(),recipientToken=login();long owner=users.get(0),recipient=users.get(1);
        var trip=trips.create(owner,new com.spvermicelli.tripledger.travel.domain.Trip(null,0,"历史","杭州",java.time.LocalDate.of(2026,9,26),java.time.LocalDate.of(2026,9,27),2,10000,"balanced","culture","metro",null,false,List.of()));tripIds.add(trip.id());
        var first=invitations.create(owner,trip.id(),recipient,"VIEWER");
        var second=invitations.create(owner,trip.id(),recipient,"EDITOR");
        jdbc.update("UPDATE tb_trip_invite SET expires_at=DATE_SUB(NOW(),INTERVAL 1 SECOND) WHERE id=?",second.id());
        var response=call(get("/api/v1/trips/"+trip.id()+"/invitations"),ownerToken);
        assertEquals(0,response.path("code").asInt());var rows=response.path("data");assertEquals(2,rows.size());
        assertEquals("EXPIRED",rows.get(0).path("status").asText());assertEquals("REVOKED",rows.get(1).path("status").asText());
        assertEquals("旅行测试",rows.get(0).path("nickname").asText());
        assertFalse(response.toString().contains("token"));assertFalse(response.toString().contains("mobile"));
        assertFalse(response.toString().contains(second.token()));
        assertEquals(1,invitations.history(owner,trip.id(),second.id()).size());
        assertEquals(first.id(),((Number)invitations.history(owner,trip.id(),second.id()).getFirst().get("id")).longValue());
        collaboration.setMember(owner,trip.id(),recipient,"EDITOR");
        assertEquals(4003,call(get("/api/v1/trips/"+trip.id()+"/invitations"),recipientToken).path("code").asInt());
        assertEquals("旅行测试",collaboration.members(recipient,trip.id()).getFirst().get("nickname"));
    }

    @Test void persistenceOwnershipVersionAndDraftLifecycle() throws Exception {
        String owner=login(),outsider=login();
        String payload="""
            {"name":"杭州两日","destination":"杭州","startDate":"2026-09-26","endDate":"2026-09-27","people":2,"budgetCent":400000,"pace":"balanced","style":"culture","stay":"metro","activities":[]}
            """;
        var created=call(post("/api/v1/trips").content(payload),owner);assertEquals(0,created.path("code").asInt(),created.toString());long id=created.path("data").path("id").asLong();tripIds.add(id);
        assertEquals(4004,call(get("/api/v1/trips/"+id),outsider).path("code").asInt());
        var updated=call(put("/api/v1/trips/"+id).content(created.path("data").toString()),owner);assertEquals(1,updated.path("data").path("version").asLong());
        assertEquals(4007,call(put("/api/v1/trips/"+id).content(created.path("data").toString()),owner).path("code").asInt());
        when(maps.search("杭州","博物馆")).thenReturn(List.of(new MapGateway.Place("p1","博物馆一","测试地址",120.1,30.2),new MapGateway.Place("p2","博物馆二","测试地址",120.2,30.3)));
        var draft=call(post("/api/v1/trips/"+id+"/plans").content("""
            {"message":"安排博物馆","people":2,"budgetCent":400000,"pace":"balanced","style":"culture","stay":"metro","version":1}
            """),owner);assertEquals(0,draft.path("code").asInt(),draft.toString());long planId=draft.path("data").path("id").asLong();
        assertEquals(0,call(get("/api/v1/trips/"+id),owner).path("data").path("activities").size());
        assertEquals(4004,call(post("/api/v1/trips/"+id+"/plans/"+planId+"/apply").content("{\"version\":1}"),outsider).path("code").asInt());
        var applied=call(post("/api/v1/trips/"+id+"/plans/"+planId+"/apply").content("{\"version\":1}"),owner);assertEquals(2,applied.path("data").path("activities").size(),applied.toString());
        assertEquals(4007,call(post("/api/v1/trips/"+id+"/plans/"+planId+"/apply").content("{\"version\":1}"),owner).path("code").asInt());
        when(maps.walk(any(),any())).thenReturn(new MapGateway.Leg(List.of(List.of(120.1,30.2),List.of(120.2,30.3)),1500,1200));
        var route=call(get("/api/v1/trips/"+id+"/route?day=0&mode=walking"),owner);assertEquals(1500,route.path("data").path("distanceMeters").asLong(),route.toString());assertEquals("GCJ-02",route.path("data").path("coordinateSystem").asText());
    }
}
