package com.spvermicelli.tripledger.billing.application.bill.command;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateBillCommand {

    private Long currentUserId;
    private Long bookId;
    private Long billId;
    private boolean fromChangeRequest;
    private String billType;
    private String title;
    private Long billAmountCent;
    private Long categoryId;
    private Long payerMemberId;
    private Long recorderMemberId;
    private Long tempParticipantId;
    private LocalDateTime billTime;
    private String remark;
    private List<String> attachmentUrls;
    private List<BillShareItemCommand> shareItems;
}
