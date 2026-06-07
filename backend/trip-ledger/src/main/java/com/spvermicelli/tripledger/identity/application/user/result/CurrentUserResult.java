package com.spvermicelli.tripledger.identity.application.user.result;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 当前用户资料结果。
 */
@Getter
@Builder
public class CurrentUserResult {
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
