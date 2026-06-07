package com.spvermicelli.tripledger.identity.interfaces.rest.user.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 当前用户资料响应。
 */
@Getter
@Builder
public class CurrentUserResponse {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String mobile;
    private String mobileMasked;
    private Boolean mobileBound;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
