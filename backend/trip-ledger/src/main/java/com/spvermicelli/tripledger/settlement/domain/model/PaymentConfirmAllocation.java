package com.spvermicelli.tripledger.settlement.domain.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 支付确认账单分配记录。
 * 该对象用于把一条成员间支付确认拆分到一笔或多笔账单上，
 * 从而支持“单账单已支付/未支付金额”的精确追溯。
 */
@Getter
@Builder
public class PaymentConfirmAllocation {

    private Long id;
    private Long paymentConfirmId;
    private Long billId;
    private Long allocatedAmountCent;
    private LocalDateTime createdAt;
}
