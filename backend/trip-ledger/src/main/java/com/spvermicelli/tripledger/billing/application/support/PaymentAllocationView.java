package com.spvermicelli.tripledger.billing.application.support;

import com.spvermicelli.tripledger.shared.domain.enums.PaymentConfirmStatus;
import lombok.Builder;
import lombok.Getter;

/**
 * 支付确认分配视图。
 * 该视图将支付确认主记录与其账单分配记录组合在一起，方便账单级统计与自动分配。
 */
@Getter
@Builder
public class PaymentAllocationView {

    private Long paymentConfirmId;
    private Long billId;
    private Long fromMemberId;
    private Long toMemberId;
    private Long allocatedAmountCent;
    private PaymentConfirmStatus confirmStatus;
}
