package com.spvermicelli.tripledger.ledger.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spvermicelli.tripledger.shared.domain.enums.InvitationStatus;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.base.BaseIdPO;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("tb_book_invitation")
@EqualsAndHashCode(callSuper = true)
public class BookInvitationPO extends BaseIdPO {
    @TableField("book_id")
    private Long bookId;
    @TableField("inviter_user_id")
    private Long inviterUserId;
    @TableField("invitee_user_id")
    private Long inviteeUserId;
    @TableField("status")
    private InvitationStatus status;
    @TableField("invited_at")
    private LocalDateTime invitedAt;
    @TableField("handled_at")
    private LocalDateTime handledAt;
    @TableField("remark")
    private String remark;
}
