package com.spvermicelli.tripledger.billing.domain.request.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BillChangeRequestSnapshot {

    private Long id;
    private Long requestId;
    private String billType;
    private String title;
    private Long billAmountCent;
    private Long categoryId;
    private Long payerMemberId;
    private Long recorderMemberId;
    private Long tempParticipantId;
    private LocalDateTime billTime;
    private String remark;
}
