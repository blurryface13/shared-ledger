package com.spvermicelli.tripledger.settlement.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spvermicelli.tripledger.shared.domain.enums.RecoveryConfirmStatus;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.base.BaseCreateTimePO;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("tb_temp_recovery_record")
@EqualsAndHashCode(callSuper = true)
public class TempRecoveryRecordPO extends BaseCreateTimePO {
    @TableField("book_id")
    private Long bookId;
    @TableField("temp_participant_id")
    private Long tempParticipantId;
    @TableField("attached_member_id")
    private Long attachedMemberId;
    @TableField("recovery_amount_cent")
    private Long recoveryAmountCent;
    @TableField("confirm_status")
    private RecoveryConfirmStatus confirmStatus;
    @TableField("confirmed_by_member_id")
    private Long confirmedByMemberId;
    @TableField("confirmed_at")
    private LocalDateTime confirmedAt;
    @TableField("remark")
    private String remark;
}
