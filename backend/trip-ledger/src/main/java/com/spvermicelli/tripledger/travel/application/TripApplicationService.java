package com.spvermicelli.tripledger.travel.application;
import com.spvermicelli.tripledger.travel.domain.*;
import com.spvermicelli.tripledger.ledger.application.book.BookApplicationService;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripApplicationService {
    private final TripRepository repository;
    private final BookApplicationService books;
    private final MapGateway maps;
    private final TripCollaborationService collaboration;
    public TripApplicationService(TripRepository repository,BookApplicationService books,MapGateway maps,TripCollaborationService collaboration){this.collaboration=collaboration;this.repository=repository;this.books=books;this.maps=maps;}
    public List<Trip> list(long user){return repository.list(user);}
    public Trip get(long user,long id){return repository.find(user,id);}
    private void binding(long user,Long bookId){if(bookId==null)return;var detail=books.getBookDetail(user,bookId);if(!detail.isCanEditBook())throw new BusinessException(ErrorCode.FORBIDDEN,"只有账本管理者可建立关联");}
    @Transactional
    public Trip create(long user,Trip trip){trip.validate();binding(user,trip.bookId());var created=repository.create(user,trip.identified(null,0));collaboration.record(user,created.id(),created.version(),"CREATED");return created;}
    @Transactional
    public Trip update(long user,long id,Trip next){collaboration.requireEditor(user,id);Trip before=get(user,id);if(before.version()!=next.version())throw new BusinessException(ErrorCode.CONFLICT,"行程已被修改，请刷新后重试");next=next.identified(id,next.version());next.validateChange(before);if(!Objects.equals(next.bookId(),before.bookId())){collaboration.requireOwner(user,id);binding(user,before.bookId());binding(user,next.bookId());}var updated=repository.update(user,next);collaboration.record(user,id,updated.version(),"UPDATED");return updated;}
    public List<MapGateway.Place> places(long user,long id,String keyword){Trip trip=get(user,id);Trip.require(keyword!=null&&!keyword.isBlank()&&keyword.length()<=100,"请填写查询地点");return maps.search(trip.destination(),keyword);}
    public record Route(String provider,String coordinateSystem,List<Trip.Activity> stops,List<List<Double>> coordinates,long distanceMeters,long durationSeconds,long version){}
    public Route route(long user,long id,int day,String mode){Trip trip=get(user,id);Trip.require(day>=0&&day<trip.days(),"日期无效");Trip.require("walking".equals(mode),"当前支持步行路线");var stops=trip.activities().stream().filter(a->a.day()==day).sorted(Comparator.comparingInt(a->Trip.time(a.time()))).toList();Trip.require(stops.size()>=2,"至少安排两个地点才能生成路线");Trip.require(stops.stream().allMatch(a->a.longitude()!=null&&a.latitude()!=null),"请先为当天所有地点补充坐标");List<List<Double>> geometry=new ArrayList<>();long distance=0,duration=0;for(int i=1;i<stops.size();i++){var leg=maps.walk(stops.get(i-1),stops.get(i));geometry.addAll(leg.coordinates());distance+=leg.distanceMeters();duration+=leg.durationSeconds();}return new Route("AMap","GCJ-02",stops,geometry,distance,duration,trip.version());}
}
