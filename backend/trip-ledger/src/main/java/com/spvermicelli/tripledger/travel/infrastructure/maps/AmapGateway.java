package com.spvermicelli.tripledger.travel.infrastructure.maps;
import com.spvermicelli.tripledger.travel.domain.*;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.fasterxml.jackson.databind.*;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/** Provider boundary: only verified POI coordinates and road geometry leave this adapter. */
@Component
public class AmapGateway implements MapGateway {
    private final String key;
    private final StringRedisTemplate redis;
    private final RestClient client;
    private final ObjectMapper json=new ObjectMapper();
    private final Semaphore permits=new Semaphore(4);
    public AmapGateway(@Value("${app.travel.amap-key:}")String key,StringRedisTemplate redis){
        this.key=key;this.redis=redis;
        var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        client=RestClient.builder().requestFactory(factory).build();
    }
    private JsonNode query(String path,Map<String,String> params){
        if(key.isBlank())throw new BusinessException(ErrorCode.INVALID_STATUS,"地图服务尚未配置，手动行程仍可保存；请配置高德 Web 服务 Key");
        String cache;
        try{cache="trip-ledger:map:v1:"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((path+new TreeMap<>(params)).getBytes(StandardCharsets.UTF_8)));}
        catch(Exception ex){throw new IllegalStateException(ex);}
        try{String cached=redis.opsForValue().get(cache);if(cached!=null)return json.readTree(cached);}catch(Exception ignored){/* Cache is optional; provider remains the source of truth. */}
        if(!permits.tryAcquire())throw new BusinessException(ErrorCode.INVALID_STATUS,"地图查询繁忙，请稍后重试");
        try{
            var uri=UriComponentsBuilder.fromUriString("https://restapi.amap.com"+path).queryParam("key",key);
            params.forEach(uri::queryParam);
            String body=client.get().uri(uri.build().encode().toUri()).retrieve().body(String.class);
            JsonNode result=json.readTree(body);
            if(!"1".equals(result.path("status").asText()))throw new BusinessException(ErrorCode.INVALID_STATUS,"地图服务查询失败，请检查服务配额与授权");
            try{redis.opsForValue().set(cache,body,Duration.ofMinutes(15));}catch(Exception ignored){}
            return result;
        }catch(BusinessException ex){throw ex;}catch(Exception ex){throw new BusinessException(ErrorCode.INVALID_STATUS,"地图服务暂不可用，请稍后重试");}
        finally{permits.release();}
    }
    public List<Place> search(String city,String keyword){
        var result=query("/v3/place/text",Map.of("city",city,"citylimit","true","keywords",keyword,"offset","20","page","1","extensions","base"));
        List<Place> places=new ArrayList<>();
        for(JsonNode p:result.path("pois")){
            String[] xy=p.path("location").asText().split(",");if(xy.length!=2)continue;
            try{double x=Double.parseDouble(xy[0]),y=Double.parseDouble(xy[1]);if(!Double.isFinite(x)||!Double.isFinite(y)||Math.abs(x)>180||Math.abs(y)>90)continue;places.add(new Place(p.path("id").asText(),p.path("name").asText(),p.path("address").asText(""),x,y));}catch(NumberFormatException ignored){}
        }
        return List.copyOf(places);
    }
    private String coordinate(Trip.Activity a){return String.format(Locale.ROOT,"%.6f,%.6f",a.longitude(),a.latitude());}
    public Leg walk(Trip.Activity from,Trip.Activity to){
        JsonNode paths=query("/v3/direction/walking",Map.of("origin",coordinate(from),"destination",coordinate(to))).path("route").path("paths");
        if(!paths.isArray()||paths.isEmpty())throw new BusinessException(ErrorCode.NOT_FOUND,"这两个地点间没有可用步行路线");
        try{
            JsonNode p=paths.get(0);long distance=Long.parseLong(p.path("distance").asText()),duration=Long.parseLong(p.path("duration").asText());
            List<List<Double>> points=new ArrayList<>();
            for(JsonNode step:p.path("steps"))for(String point:step.path("polyline").asText().split(";")){String[] xy=point.split(",");if(xy.length==2)points.add(List.of(Double.parseDouble(xy[0]),Double.parseDouble(xy[1])));}
            if(points.size()<2||distance<0||duration<0)throw new IllegalArgumentException();
            return new Leg(List.copyOf(points),distance,duration);
        }catch(Exception ex){throw new BusinessException(ErrorCode.INVALID_STATUS,"地图返回的路线数据不完整，请重试");}
    }
}
