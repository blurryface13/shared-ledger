package com.spvermicelli.tripledger.travel.infrastructure.maps;

import com.spvermicelli.tripledger.travel.domain.MapGateway;
import com.spvermicelli.tripledger.travel.domain.Trip;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** OpenStreetMap provider. All public-service requests are explicit, bounded and cached. */
@Component
public class OsmGateway implements MapGateway {
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private final ObjectMapper json=new ObjectMapper();
    private final StringRedisTemplate redis;
    private final String userAgent;
    private final Semaphore geocoder=new Semaphore(1), other=new Semaphore(1);
    private final Map<String,Cache> local=new ConcurrentHashMap<>();
    private long lastGeocodeAt=0;
    private record Cache(JsonNode value,long expiresAt){}
    public OsmGateway(StringRedisTemplate redis,@Value("${app.travel.osm-user-agent:TripLedger/0.1 (local development)}")String userAgent){this.redis=redis;this.userAgent=userAgent;}
    private static String enc(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
    private static String pair(double longitude,double latitude){return String.format(Locale.ROOT,"%.6f,%.6f",longitude,latitude);}
    private static boolean valid(double lon,double lat){return Double.isFinite(lon)&&Double.isFinite(lat)&&Math.abs(lon)<=180&&Math.abs(lat)<=90;}
    private JsonNode request(String id,URI uri,String body,Duration ttl,boolean nominatim){
        String key="trip-ledger:osm:v1:"+id;
        Cache cached=local.get(key);if(cached!=null&&cached.expiresAt>System.currentTimeMillis())return cached.value;
        try{String value=redis.opsForValue().get(key);if(value!=null){JsonNode tree=json.readTree(value);local.put(key,new Cache(tree,System.currentTimeMillis()+Math.min(ttl.toMillis(),60000)));return tree;}}catch(Exception ignored){/* Redis is an optional cache. */}
        Semaphore permit=nominatim?geocoder:other;
        if(!permit.tryAcquire())throw new BusinessException(ErrorCode.INVALID_STATUS,"地图查询繁忙，请稍后重试");
        try{
            // Nominatim's public server permits at most one request per second per application.
            if(nominatim){long wait=1200-(System.currentTimeMillis()-lastGeocodeAt);if(wait>0)Thread.sleep(wait);lastGeocodeAt=System.currentTimeMillis();}
            HttpRequest.Builder builder=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(nominatim?8:18)).header("User-Agent",userAgent).header("Accept","application/json");
            HttpRequest req=body==null?builder.GET().build():builder.header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> result=client.send(req,HttpResponse.BodyHandlers.ofString());
            if(result.statusCode()!=200)throw new BusinessException(ErrorCode.INVALID_STATUS,"地点服务暂不可用，请稍后重试");
            JsonNode tree=json.readTree(result.body());
            local.put(key,new Cache(tree,System.currentTimeMillis()+ttl.toMillis()));
            try{redis.opsForValue().set(key,result.body(),ttl);}catch(Exception ignored){}
            return tree;
        }catch(BusinessException ex){throw ex;}
        catch(InterruptedException ex){Thread.currentThread().interrupt();throw new BusinessException(ErrorCode.INVALID_STATUS,"地图查询已中断");}
        catch(Exception ex){throw new BusinessException(ErrorCode.INVALID_STATUS,"地图服务暂不可用，请稍后重试");}
        finally{permit.release();}
    }
    private JsonNode nominatim(String id,String params){return request(id,URI.create("https://nominatim.openstreetmap.org/search?format=jsonv2&addressdetails=1&accept-language=zh-CN&"+params),null,Duration.ofHours(24),true);}
    public Center center(String city){
        JsonNode found=nominatim("center:"+city,"city="+enc(city)+"&limit=1");
        if(!found.isArray()||found.isEmpty())throw new BusinessException(ErrorCode.NOT_FOUND,"无法确定目的地位置，请搜索具体地点");
        double lat=found.get(0).path("lat").asDouble(Double.NaN),lon=found.get(0).path("lon").asDouble(Double.NaN);
        if(!valid(lon,lat))throw new BusinessException(ErrorCode.INVALID_STATUS,"地点坐标无效");
        return new Center(lon,lat);
    }
    public List<Place> search(String city,String keyword){
        Center c=center(city);double lon=c.longitude(),lat=c.latitude();
        String box=String.format(Locale.ROOT,"%.4f,%.4f,%.4f,%.4f",lon-.35,lat+.35,lon+.35,lat-.35);
        JsonNode found=nominatim("search:"+city+":"+keyword,"q="+enc(keyword)+"&viewbox="+enc(box)+"&bounded=1&limit=20");
        List<Place> places=new ArrayList<>();
        for(JsonNode p:found){
            double x=p.path("lon").asDouble(Double.NaN),y=p.path("lat").asDouble(Double.NaN);if(!valid(x,y))continue;
            String name=p.path("name").asText("").trim();if(name.isEmpty())name=p.path("display_name").asText("").split(",")[0];
            if(name.isBlank())continue;
            String id=p.path("osm_type").asText()+"/"+p.path("osm_id").asText();
            places.add(new Place(id,name,p.path("display_name").asText(""),x,y));
        }
        return List.copyOf(places);
    }
    public List<Place> nearby(double longitude,double latitude,String category){
        String term=switch(category){case "sights"->"attraction";case "stays"->"hotel";case "food"->"restaurant";default->throw new BusinessException(ErrorCode.INVALID_PARAM,"地点类型无效");};
        double latDelta=3000d/111000d,lonDelta=latDelta/Math.max(.2,Math.cos(Math.toRadians(latitude)));
        String box=String.format(Locale.ROOT,"%.5f,%.5f,%.5f,%.5f",longitude-lonDelta,latitude+latDelta,longitude+lonDelta,latitude-latDelta);
        JsonNode found=nominatim("nearby:"+pair(longitude,latitude)+":"+category,"q="+enc(term)+"&viewbox="+enc(box)+"&bounded=1&limit=40");
        List<Place> places=new ArrayList<>();
        for(JsonNode p:found){
            double x=p.path("lon").asDouble(Double.NaN),y=p.path("lat").asDouble(Double.NaN);if(!valid(x,y)||distance(longitude,latitude,x,y)>3000)continue;
            String name=p.path("name").asText("").trim();if(name.isBlank())name=p.path("display_name").asText("").split(",")[0];if(name.isBlank())continue;
            String id=p.path("osm_type").asText()+"/"+p.path("osm_id").asText();
            places.add(new Place(id,name,p.path("display_name").asText(""),x,y));
        }
        places.sort(Comparator.comparingDouble(p->distance(longitude,latitude,p.longitude(),p.latitude())));
        return List.copyOf(places.subList(0,Math.min(20,places.size())));
    }
    private static double distance(double x,double y,double x2,double y2){double r=Math.PI/180,a=Math.sin((y2-y)*r/2),b=Math.sin((x2-x)*r/2);return 2*6371000*Math.asin(Math.sqrt(a*a+Math.cos(y*r)*Math.cos(y2*r)*b*b));}
    private static double[] wgs(Trip.Activity a){
        double x=a.longitude(),y=a.latitude();if("WGS84".equals(a.coordinateSystem())||x<72.004||x>137.8347||y<.8293||y>55.8271)return new double[]{x,y};
        for(int i=0;i<5;i++){double[] d=offset(x,y);x=a.longitude()-d[0];y=a.latitude()-d[1];}return new double[]{x,y};
    }
    private static double[] offset(double lon,double lat){
        double x=lon-105,y=lat-35,pi=Math.PI;
        double a=-100+2*x+3*y+.2*y*y+.1*x*y+.2*Math.sqrt(Math.abs(x));
        double b=300+x+2*y+.1*x*x+.1*x*y+.1*Math.sqrt(Math.abs(x));
        double wave=(20*Math.sin(6*x*pi)+20*Math.sin(2*x*pi))*2/3;
        a+=wave+(20*Math.sin(y*pi)+40*Math.sin(y*pi/3))*2/3+(160*Math.sin(y*pi/12)+320*Math.sin(y*pi/30))*2/3;
        b+=wave+(20*Math.sin(x*pi)+40*Math.sin(x*pi/3))*2/3+(150*Math.sin(x*pi/12)+300*Math.sin(x*pi/30))*2/3;
        double rad=lat*pi/180,m=1-.00669342162296594323*Math.pow(Math.sin(rad),2);
        return new double[]{b*180/(6378245/Math.sqrt(m)*Math.cos(rad)*pi),a*180/((6378245*(1-.00669342162296594323))/(m*Math.sqrt(m))*pi)};
    }
    public Leg walk(Trip.Activity from,Trip.Activity to){
        double[] a=wgs(from),b=wgs(to);
        String points=pair(a[0],a[1])+";"+pair(b[0],b[1]);
        JsonNode r=request("foot:"+points,URI.create("https://routing.openstreetmap.de/routed-foot/route/v1/driving/"+points+"?overview=full&geometries=geojson"),null,Duration.ofMinutes(20),false);
        JsonNode routes=r.path("routes");if(!"Ok".equals(r.path("code").asText())||!routes.isArray()||routes.isEmpty())throw new BusinessException(ErrorCode.NOT_FOUND,"这两个地点间没有可用步行路线");
        JsonNode waypoints=r.path("waypoints");
        if(!waypoints.isArray()||waypoints.size()<2)throw new BusinessException(ErrorCode.INVALID_STATUS,"路线端点无法校验，请重试");
        if(waypoints.get(0).path("distance").asDouble(Double.POSITIVE_INFINITY)>250)throw new BusinessException(ErrorCode.INVALID_PARAM,"「"+from.title()+"」离可步行道路较远，请选择具体入口或码头");
        if(waypoints.get(1).path("distance").asDouble(Double.POSITIVE_INFINITY)>250)throw new BusinessException(ErrorCode.INVALID_PARAM,"「"+to.title()+"」离可步行道路较远，请选择具体入口或码头");
        JsonNode route=routes.get(0);List<List<Double>> coordinates=new ArrayList<>();
        for(JsonNode point:route.path("geometry").path("coordinates")){if(point.size()<2)continue;double x=point.get(0).asDouble(Double.NaN),y=point.get(1).asDouble(Double.NaN);if(valid(x,y))coordinates.add(List.of(x,y));}
        if(coordinates.size()<2)throw new BusinessException(ErrorCode.INVALID_STATUS,"路线坐标不完整，请重试");
        return new Leg(List.copyOf(coordinates),Math.round(route.path("distance").asDouble()),Math.round(route.path("duration").asDouble()));
    }
}
