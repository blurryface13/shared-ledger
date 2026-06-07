package com.spvermicelli.tripledger.settlement.interfaces.rest.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentConfirmResponse {
    private Long paymentConfirmId;
    private Long bookId;
    private Long fromMemberId;
    private String fromMemberName;
    private Long toMemberId;
    private String toMemberName;
    private Long paymentAmountCent;
    private String sourceType;
    private Long sourceRefId;
    private String confirmStatus;
    private Long initiatedByMemberId;
    private Long confirmedByMemberId;
    private LocalDateTime initiatedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime autoConfirmAt;
    private String remark;
    private List<PaymentConfirmAllocationResponse> allocationList;
}
