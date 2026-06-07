package com.spvermicelli.tripledger.billing.application.request.command;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifyBillPayload {

    private String billType;
    private String title;
    private Long billAmountCent;
    private Long categoryId;
    private Long payerMemberId;
    private Long recorderMemberId;
    private Long tempParticipantId;
    private LocalDateTime billTime;
    private LocalDateTime snapshotEditedAt;
    private String remark;
    private List<ModifyBillShareItemPayload> shareItems;
}
