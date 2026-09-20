package com.spvermicelli.tripledger.travel.interfaces.rest;
import com.spvermicelli.tripledger.travel.application.TripInvitationService;
import com.spvermicelli.tripledger.shared.common.context.UserContextHolder;
import com.spvermicelli.tripledger.shared.common.response.ApiResponse;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/trips")
public class TripInvitationController {
 private final TripInvitationService service;
 public TripInvitationController(TripInvitationService service){this.service=service;}
 @GetMapping("/{id}/invitations") public ApiResponse<?> history(@PathVariable long id,@RequestParam(defaultValue="0") long before){return ApiResponse.success(service.history(UserContextHolder.getUserId(),id,before));}
 public record Create(long targetUserId,String role){}
 public record Accept(String token){}
 @PostMapping("/{id}/invitations") public ApiResponse<?> create(@PathVariable long id,@RequestBody Create request){return ApiResponse.success(service.create(UserContextHolder.getUserId(),id,request.targetUserId(),request.role()));}
 @DeleteMapping("/{id}/invitations/{invite}") public ApiResponse<?> revoke(@PathVariable long id,@PathVariable long invite){service.revoke(UserContextHolder.getUserId(),id,invite);return ApiResponse.success();}
 @PostMapping("/invitations/accept") public ApiResponse<?> accept(@RequestBody Accept request){return ApiResponse.success(service.accept(UserContextHolder.getUserId(),request.token()));}
}
