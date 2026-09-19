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
    @AfterEach void cleanup(){for(Long id:tripIds){jdbc.update("DELETE FROM tb_trip_plan WHERE trip_id=?",id);jdbc.update("DELETE FROM tb_trip WHERE id=?",id);}for(Long id:users){jdbc.update("DELETE FROM tb_user_refresh_token WHERE user_id=?",id);jdbc.update("DELETE FROM tb_user WHERE id=?",id);}}
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
