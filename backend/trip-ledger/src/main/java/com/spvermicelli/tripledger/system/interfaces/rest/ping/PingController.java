package com.spvermicelli.tripledger.system.interfaces.rest.ping;

import com.spvermicelli.tripledger.shared.common.context.UserContextHolder;
import com.spvermicelli.tripledger.shared.common.response.ApiResponse;
import com.spvermicelli.tripledger.system.interfaces.rest.ping.response.PingResponse;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PingController {

    private final Environment environment;

    public PingController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/ping")
    public ApiResponse<PingResponse> ping() {
        String[] activeProfiles = environment.getActiveProfiles();
        String activeProfile = activeProfiles.length > 0 ? activeProfiles[0] : environment.getDefaultProfiles()[0];

        return ApiResponse.success(PingResponse.builder()
            .status("UP")
            .application("trip-ledger")
            .activeProfile(activeProfile)
            .serverTime(LocalDateTime.now())
            .currentUserId(UserContextHolder.getUserId())
            .build());
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("status", "UP"));
    }
}
