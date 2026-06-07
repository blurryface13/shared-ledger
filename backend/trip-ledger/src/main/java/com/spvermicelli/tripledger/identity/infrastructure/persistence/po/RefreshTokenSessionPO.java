package com.spvermicelli.tripledger.identity.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spvermicelli.tripledger.identity.domain.auth.valueobject.RefreshTokenStatus;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.base.BaseAuditPO;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * refresh token 会话持久化对象。
 */
@Data
@TableName("tb_user_refresh_token")
@EqualsAndHashCode(callSuper = true)
public class RefreshTokenSessionPO extends BaseAuditPO {

    @TableField("user_id")
    private Long userId;

    @TableField("token_hash")
    private String tokenHash;

    @TableField("status")
    private RefreshTokenStatus status;

    @TableField("expire_at")
    private LocalDateTime expireAt;

    @TableField("last_used_at")
    private LocalDateTime lastUsedAt;

    @TableField("revoked_at")
    private LocalDateTime revokedAt;

    @TableField("replaced_by_token_hash")
    private String replacedByTokenHash;
}
