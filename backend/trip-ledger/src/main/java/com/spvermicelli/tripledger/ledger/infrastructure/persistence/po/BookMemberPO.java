package com.spvermicelli.tripledger.ledger.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spvermicelli.tripledger.shared.domain.enums.MemberRole;
import com.spvermicelli.tripledger.shared.domain.enums.MemberStatus;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.base.BaseAuditPO;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("tb_book_member")
@EqualsAndHashCode(callSuper = true)
public class BookMemberPO extends BaseAuditPO {
    @TableField("book_id")
    private Long bookId;
    @TableField("user_id")
    private Long userId;
    @TableField("budget_amount_cent")
    private Long budgetAmountCent;
    @TableField("member_role")
    private MemberRole memberRole;
    @TableField("member_status")
    private MemberStatus memberStatus;
    @TableField("joined_at")
    private LocalDateTime joinedAt;
    @TableField("left_at")
    private LocalDateTime leftAt;
    @TableField("invited_by_user_id")
    private Long invitedByUserId;
}
