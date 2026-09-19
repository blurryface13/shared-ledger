package com.spvermicelli.tripledger.travel.application;
import com.spvermicelli.tripledger.travel.domain.*;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lightweight intent routing and deterministic scheduling; no model may write domain data directly. */
@Service
public class PlanningApplicationService {
    public record Request(String message,int people,long budgetCent,String pace,String style,String stay,long version){}
    public record Draft(Long id,String intent,String explanation,List<Trip.Activity> activities,int people,long budgetCent,String pace,String style,String stay){}
    private final TripApplicationService trips;private final TripRepository repository;private final MapGateway maps;
    private final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
    public PlanningApplicationService(TripApplicationService trips,TripRepository repository,MapGateway maps){this.trips=trips;this.repository=repository;this.maps=maps;}
    public Draft generate(long user,long id,Request r){
        Trip trip=trips.get(user,id);Trip.require(r.message()!=null&&!r.message().isBlank()&&r.message().length()<=1000,"请填写规划需求");
        if(trip.archived())throw new BusinessException(ErrorCode.INVALID_STATUS,"请先恢复行程");
        if(trip.version()!=r.version())throw new BusinessException(ErrorCode.CONFLICT,"行程已变化，请刷新后生成");
        trip.planned(trip.activities(),r.people(),r.budgetCent(),r.pace(),r.style(),r.stay()).validate();
        if(r.message().contains("查看")||r.message().contains("查询"))return new Draft(null,"QUERY","当前有 "+trip.activities().size()+" 个安排；查询不会修改行程。",List.of(),r.people(),r.budgetCent(),r.pace(),r.style(),r.stay());
        String keyword=switch(r.style()){case "culture"->"博物馆";case "nature"->"公园";default->"景点";};
        var candidates=maps.search(trip.destination(),keyword);
        if(candidates.isEmpty())throw new BusinessException(ErrorCode.NOT_FOUND,"未查到合适地点，请调整目的地或手动安排");
        List<Trip.Activity> items=new ArrayList<>(trip.activities().stream().filter(a->a.locked()||a.done()).toList());
        Set<String> used=new HashSet<>();items.forEach(a->{if(a.poiId()!=null)used.add(a.poiId());});
        int perDay=switch(r.pace()){case "relaxed"->2;case "packed"->4;default->3;};
        int cursor=0;
        for(int d=0;d<trip.days();d++)for(int slot=0;slot<perDay;slot++){
            int minute=9*60+slot*180,day=d;
            if(items.stream().anyMatch(a->a.day()==day&&minute<Trip.time(a.time())+a.duration()+30&&minute+120+30>Trip.time(a.time())))continue;
            while(cursor<candidates.size()&&used.contains(candidates.get(cursor).id()))cursor++;
            if(cursor>=candidates.size())break;
            var p=candidates.get(cursor++);used.add(p.id());items.add(new Trip.Activity(UUID.randomUUID().toString(),d,p.name(),String.format("%02d:%02d",minute/60,minute%60),120,"营业、票价与预约待确认","blue",false,false,p.id(),p.longitude(),p.latitude()));
        }
        items.sort(Comparator.comparingInt(Trip.Activity::day).thenComparingInt(a->Trip.time(a.time())));
        trip.planned(items,r.people(),r.budgetCent(),r.pace(),r.style(),r.stay()).validate();
        Draft draft=new Draft(null,trip.activities().isEmpty()?"CREATE":"REPLAN","基于真实地点候选生成草案，保留锁定和打卡安排。预算与住宿偏好已记录，但尚未核验价格、营业时间及交通可达性；空闲时段含待确认交通。",List.copyOf(items),r.people(),r.budgetCent(),r.pace(),r.style(),r.stay());
        try{long planId=repository.storePlan(user,id,trip.version(),json.writeValueAsString(draft));return new Draft(planId,draft.intent(),draft.explanation(),draft.activities(),draft.people(),draft.budgetCent(),draft.pace(),draft.style(),draft.stay());}catch(BusinessException ex){throw ex;}catch(Exception ex){throw new IllegalStateException(ex);}
    }
    @Transactional
    public Trip apply(long user,long id,long planId,long version){
        Trip current=trips.get(user,id);if(current.version()!=version)throw new BusinessException(ErrorCode.CONFLICT,"行程已变化，请重新规划");
        try{Draft d=json.readValue(repository.getPlan(user,id,planId,version),Draft.class);Trip result=trips.update(user,id,current.planned(d.activities(),d.people(),d.budgetCent(),d.pace(),d.style(),d.stay()));repository.applied(planId);return result;}catch(BusinessException ex){throw ex;}catch(Exception ex){throw new IllegalStateException(ex);}
    }
}
