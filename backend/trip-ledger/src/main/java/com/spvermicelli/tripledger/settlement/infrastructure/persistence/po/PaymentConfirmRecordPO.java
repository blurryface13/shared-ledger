package com.spvermicelli.tripledger.settlement.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spvermicelli.tripledger.shared.domain.enums.PaymentConfirmStatus;
import com.spvermicelli.tripledger.shared.domain.enums.PaymentSourceType;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.base.BaseIdPO;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("tb_payment_confirm_record")
@EqualsAndHashCode(callSuper = true)
public class PaymentConfirmRecordPO extends BaseIdPO {
    @TableField("book_id")
    private Long bookId;
    @TableField("from_member_id")
    private Long fromMemberId;
    @TableField("to_member_id")
    private Long toMemberId;
    @TableField("payment_amount_cent")
    private Long paymentAmountCent;
    @TableField("source_type")
    private PaymentSourceType sourceType;
    @TableField("source_ref_id")
    private Long sourceRefId;
    @TableField("confirm_status")
    private PaymentConfirmStatus confirmStatus;
    @TableField("initiated_by_member_id")
    private Long initiatedByMemberId;
    @TableField("confirmed_by_member_id")
    private Long confirmedByMemberId;
    @TableField("initiated_at")
    private LocalDateTime initiatedAt;
    @TableField("confirmed_at")
    private LocalDateTime confirmedAt;
    @TableField("auto_confirm_at")
    private LocalDateTime autoConfirmAt;
    @TableField("remark")
    private String remark;
}
