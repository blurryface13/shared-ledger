package com.spvermicelli.tripledger.system.interfaces.rest.ping.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PingResponse {
    private String status;
    private String application;
    private String activeProfile;
    private LocalDateTime serverTime;
    private Long currentUserId;
}
