package com.spvermicelli.tripledger.identity.domain.auth.model;

import com.spvermicelli.tripledger.identity.domain.auth.valueobject.RefreshTokenStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * refresh token 持久化会话。
 * 这里不保存明文 refresh token，只保存其摘要值，降低数据库泄漏后的风险。
 */
@Getter
@Builder
public class RefreshTokenSession {
    // 数据库 id
    private Long id;
    // user id
    private Long userId;
    // refresh token 的哈希，不是明文
    private String tokenHash;
    // refresh token 的状态
    private RefreshTokenStatus status;
    // refresh token 过期时间
    private Instant expireAt;
    // refresh token 上次使用时间
    private Instant lastUsedAt;
    // refresh token 撤销时间
    private Instant revokedAt;
    // refresh token 续签后新的token
    private String replacedByTokenHash;
    // 创建时间
    private LocalDateTime createdAt;
    // 更新时间
    private LocalDateTime updatedAt;

    public RefreshTokenSession markUsed(String nextTokenHash) {
        return RefreshTokenSession.builder()
            .id(id)
            .userId(userId)
            .tokenHash(tokenHash)
            .status(RefreshTokenStatus.USED)
            .expireAt(expireAt)
            .lastUsedAt(Instant.now())
            .revokedAt(revokedAt)
            .replacedByTokenHash(nextTokenHash)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }

    public RefreshTokenSession markExpired() {
        return RefreshTokenSession.builder()
            .id(id)
            .userId(userId)
            .tokenHash(tokenHash)
            .status(RefreshTokenStatus.EXPIRED)
            .expireAt(expireAt)
            .lastUsedAt(lastUsedAt)
            .revokedAt(revokedAt)
            .replacedByTokenHash(replacedByTokenHash)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }

    /**
     * 主动撤销 refresh token。
     * 用于注销账号、风控踢下线等需要立即终止会话的场景。
     */
    public RefreshTokenSession markRevoked() {
        return RefreshTokenSession.builder()
            .id(id)
            .userId(userId)
            .tokenHash(tokenHash)
            .status(RefreshTokenStatus.REVOKED)
            .expireAt(expireAt)
            .lastUsedAt(lastUsedAt)
            .revokedAt(Instant.now())
            .replacedByTokenHash(replacedByTokenHash)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }
}
