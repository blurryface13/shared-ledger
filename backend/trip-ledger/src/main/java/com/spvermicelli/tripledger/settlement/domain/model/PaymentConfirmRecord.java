package com.spvermicelli.tripledger.settlement.domain.model;

import com.spvermicelli.tripledger.shared.domain.enums.PaymentConfirmStatus;
import com.spvermicelli.tripledger.shared.domain.enums.PaymentSourceType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 支付确认记录。
 * V1 中付款方和收款方之间的资金流转统一通过该对象承载，
 * 既支持来源于结算建议的支付，也支持手工补款。
 */
@Getter
@Builder
public class PaymentConfirmRecord {

    private Long id;
    private Long bookId;
    private Long fromMemberId;
    private Long toMemberId;
    private Long paymentAmountCent;
    private PaymentSourceType sourceType;
    private Long sourceRefId;
    private PaymentConfirmStatus confirmStatus;
    private Long initiatedByMemberId;
    private Long confirmedByMemberId;
    private LocalDateTime initiatedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime autoConfirmAt;
    private String remark;

    public boolean isPendingConfirm() {
        return confirmStatus == PaymentConfirmStatus.PENDING_CONFIRM;
    }

    public boolean isConfirmedLike() {
        return confirmStatus == PaymentConfirmStatus.CONFIRMED
            || confirmStatus == PaymentConfirmStatus.AUTO_CONFIRMED;
    }

    public PaymentConfirmRecord confirm(Long confirmerMemberId, LocalDateTime now) {
        return copy(PaymentConfirmStatus.CONFIRMED, confirmerMemberId, now, null);
    }

    public PaymentConfirmRecord autoConfirm(Long confirmerMemberId, LocalDateTime now) {
        return copy(PaymentConfirmStatus.AUTO_CONFIRMED, confirmerMemberId, now, now);
    }

    public PaymentConfirmRecord reject(Long confirmerMemberId, LocalDateTime now) {
        return copy(PaymentConfirmStatus.REJECT, confirmerMemberId, now, null);
    }

    private PaymentConfirmRecord copy(
        PaymentConfirmStatus nextStatus,
        Long confirmerMemberId,
        LocalDateTime confirmedTime,
        LocalDateTime autoConfirmedTime
    ) {
        return PaymentConfirmRecord.builder()
            .id(id)
            .bookId(bookId)
            .fromMemberId(fromMemberId)
            .toMemberId(toMemberId)
            .paymentAmountCent(paymentAmountCent)
            .sourceType(sourceType)
            .sourceRefId(sourceRefId)
            .confirmStatus(nextStatus)
            .initiatedByMemberId(initiatedByMemberId)
            .confirmedByMemberId(confirmerMemberId)
            .initiatedAt(initiatedAt)
            .confirmedAt(confirmedTime)
            .autoConfirmAt(autoConfirmedTime)
            .remark(remark)
            .build();
    }
}
