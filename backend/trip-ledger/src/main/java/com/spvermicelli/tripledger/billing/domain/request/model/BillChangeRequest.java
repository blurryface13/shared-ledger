package com.spvermicelli.tripledger.billing.domain.request.model;

import com.spvermicelli.tripledger.shared.domain.enums.ChangeRequestStatus;
import com.spvermicelli.tripledger.shared.domain.enums.ChangeRequestType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 账单修改/删除申请聚合。
 * V1 中审批流采用“任一有效审批主体同意即可生效”的策略，
 * 因此申请本身是状态载体，审批动作作为审计子记录单独保存。
 */
@Getter
@Builder
public class BillChangeRequest {

    private Long id;
    private Long billId;
    private Long predecessorRequestId;
    private boolean baseline;
    private ChangeRequestType requestType;
    private Long requesterMemberId;
    private String requestReason;
    private ChangeRequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime handledAt;

    public boolean isPending() {
        return status == ChangeRequestStatus.PENDING;
    }

    public BillChangeRequest approve(LocalDateTime now) {
        return copyWith(ChangeRequestStatus.APPROVED, now);
    }

    public BillChangeRequest reject(LocalDateTime now) {
        return copyWith(ChangeRequestStatus.REJECTED, now);
    }

    public BillChangeRequest cancel(LocalDateTime now) {
        return copyWith(ChangeRequestStatus.CANCELLED, now);
    }

    public BillChangeRequest obsolete(LocalDateTime now) {
        return copyWith(ChangeRequestStatus.OUTDATED, now);
    }

    private BillChangeRequest copyWith(ChangeRequestStatus nextStatus, LocalDateTime now) {
        return BillChangeRequest.builder()
            .id(id)
            .billId(billId)
            .predecessorRequestId(predecessorRequestId)
            .baseline(baseline)
            .requestType(requestType)
            .requesterMemberId(requesterMemberId)
            .requestReason(requestReason)
            .status(nextStatus)
            .createdAt(createdAt)
            .handledAt(now)
            .build();
    }
}
