package com.spvermicelli.tripledger.travel.interfaces.rest;
import com.spvermicelli.tripledger.travel.domain.*;
import com.spvermicelli.tripledger.travel.application.*;
import com.spvermicelli.tripledger.shared.common.context.UserContextHolder;
import com.spvermicelli.tripledger.shared.common.response.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/trips")
public class TripController {
    private final TripApplicationService trips;private final PlanningApplicationService planner;
    public TripController(TripApplicationService trips,PlanningApplicationService planner){this.trips=trips;this.planner=planner;}
    private long user(){return UserContextHolder.getUserId();}
    @GetMapping public ApiResponse<List<Trip>> list(){return ApiResponse.success(trips.list(user()));}
    public record CreateTrip(String name,String destination,java.time.LocalDate startDate,java.time.LocalDate endDate,int people,long budgetCent,String pace,String style,String stay,Long bookId,List<Trip.Activity> activities) {}
    @PostMapping public ApiResponse<Trip> create(@RequestBody CreateTrip request){return ApiResponse.success(trips.create(user(),new Trip(null,0,request.name(),request.destination(),request.startDate(),request.endDate(),request.people(),request.budgetCent(),request.pace(),request.style(),request.stay(),request.bookId(),false,request.activities())));}
    @GetMapping("/{id}") public ApiResponse<Trip> get(@PathVariable long id){return ApiResponse.success(trips.get(user(),id));}
    @PutMapping("/{id}") public ApiResponse<Trip> update(@PathVariable long id,@RequestBody Trip trip){return ApiResponse.success(trips.update(user(),id,trip));}
    @GetMapping("/{id}/places") public ApiResponse<List<MapGateway.Place>> places(@PathVariable long id,@RequestParam String keyword){return ApiResponse.success(trips.places(user(),id,keyword));}
    @GetMapping("/{id}/route") public ApiResponse<TripApplicationService.Route> route(@PathVariable long id,@RequestParam int day,@RequestParam(defaultValue="walking")String mode){return ApiResponse.success(trips.route(user(),id,day,mode));}
    @PostMapping("/{id}/plans") public ApiResponse<PlanningApplicationService.Draft> plan(@PathVariable long id,@RequestBody PlanningApplicationService.Request request){return ApiResponse.success(planner.generate(user(),id,request));}
    public record Apply(long version){}
    @PostMapping("/{id}/plans/{planId}/apply") public ApiResponse<Trip> apply(@PathVariable long id,@PathVariable long planId,@RequestBody Apply request){return ApiResponse.success(planner.apply(user(),id,planId,request.version()));}
}
