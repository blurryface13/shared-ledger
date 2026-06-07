package com.spvermicelli.tripledger.identity.application.auth.result;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MockLoginUserResult {

    private Long userId;
    private String openId;
    private String suggestedCode;
    private String nickname;
    private Boolean mobileBound;
    private String mobileMasked;
    private String status;
    private LocalDateTime updatedAt;
}
