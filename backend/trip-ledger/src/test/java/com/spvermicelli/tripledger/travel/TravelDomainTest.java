package com.spvermicelli.tripledger.travel;
import com.spvermicelli.tripledger.travel.domain.*;
import com.spvermicelli.tripledger.travel.application.*;
import com.spvermicelli.tripledger.travel.infrastructure.persistence.*;
import com.spvermicelli.tripledger.travel.infrastructure.persistence.mapper.TripMapper;
import com.spvermicelli.tripledger.ledger.application.book.BookApplicationService;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TravelDomainTest {
    private Trip trip(List<Trip.Activity> a){return new Trip(1L,2,"杭州","杭州",LocalDate.of(2026,9,26),LocalDate.of(2026,9,27),2,400000,"balanced","classic","metro",null,false,a);}
    private Trip.Activity activity(String id,String time,boolean locked){return new Trip.Activity(id,0,id,time,90,"","blue",locked,false,null,null,null);}
    @Test void rejectsOverlappingScheduleAndDuplicateIds(){assertThrows(BusinessException.class,()->trip(List.of(activity("a","09:00",false),activity("b","10:00",false))).validate());assertThrows(BusinessException.class,()->trip(List.of(activity("a","09:00",false),activity("a","12:00",false))).validate());}
    @Test void lockedActivityCannotBeRemovedOrMovedButMayBeUnlocked(){Trip before=trip(List.of(activity("a","09:00",true)));assertThrows(BusinessException.class,()->trip(List.of()).validateChange(before));assertThrows(BusinessException.class,()->trip(List.of(activity("a","10:00",true))).validateChange(before));assertDoesNotThrow(()->trip(List.of(activity("a","09:00",false))).validateChange(before));}
    @Test void staleVersionNeverWrites(){var repo=mock(TripRepository.class);when(repo.find(7,1)).thenReturn(trip(List.of()));var service=new TripApplicationService(repo,mock(BookApplicationService.class),mock(MapGateway.class),mock(TripCollaborationService.class));assertThrows(BusinessException.class,()->service.update(7,1,trip(List.of()).identified(1L,1)));verify(repo,never()).update(anyLong(),any());}
    @Test void routeRequiresCoordinatesBeforeCallingProvider(){var repo=mock(TripRepository.class);var maps=mock(MapGateway.class);when(repo.find(7,1)).thenReturn(trip(List.of(activity("a","09:00",false),activity("b","12:00",false))));var service=new TripApplicationService(repo,mock(BookApplicationService.class),maps,mock(TripCollaborationService.class));assertThrows(BusinessException.class,()->service.route(7,1,0,"walking"));verifyNoInteractions(maps);}
    @Test void databaseCompareAndSwapRejectsConcurrentWinner(){var mapper=mock(TripMapper.class);when(mapper.update(any())).thenReturn(0);var repository=new MybatisTripRepository(mapper);assertThrows(BusinessException.class,()->repository.update(7,trip(List.of())));}
    @Test void planningPreservesLockedStopsAndQueriesDoNotWrite(){var trips=mock(TripApplicationService.class);var repo=mock(TripRepository.class);var maps=mock(MapGateway.class);Trip before=trip(List.of(activity("locked","09:00",true)));when(trips.get(7,1)).thenReturn(before);when(maps.search("杭州","景点")).thenReturn(List.of(new MapGateway.Place("p","真实候选","地址",120.1,30.2)));when(repo.storePlan(anyLong(),anyLong(),anyLong(),anyString())).thenReturn(9L);var service=new PlanningApplicationService(trips,repo,maps);var request=new PlanningApplicationService.Request("重新安排",2,400000,"balanced","classic","metro",2);var draft=service.generate(7,1,request);assertTrue(draft.activities().contains(before.activities().getFirst()));assertTrue(draft.activities().stream().anyMatch(a->"p".equals(a.poiId())));clearInvocations(repo,maps);var query=service.generate(7,1,new PlanningApplicationService.Request("查看行程",2,400000,"balanced","classic","metro",2));assertEquals("QUERY",query.intent());verifyNoInteractions(repo,maps);}
}
