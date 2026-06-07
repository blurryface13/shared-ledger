package com.spvermicelli.tripledger.billing.application.support;

import lombok.Builder;
import lombok.Getter;

/**
 * 支付确认自动分配草稿。
 * 在持久化支付确认前先计算出应分配到哪些账单、各分配多少金额，
 * 再由应用服务统一落库。
 */
@Getter
@Builder
public class PaymentAllocationDraft {

    private Long billId;
    private Long allocatedAmountCent;
}
