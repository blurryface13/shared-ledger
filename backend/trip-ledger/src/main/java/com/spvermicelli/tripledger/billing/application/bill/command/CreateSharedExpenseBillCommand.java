package com.spvermicelli.tripledger.billing.application.bill.command;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateSharedExpenseBillCommand {

    private Long currentUserId;
    private Long bookId;
    private String title;
    private Long billAmountCent;
    private Long categoryId;
    private Long payerMemberId;
    private Long recorderMemberId;
    private LocalDateTime billTime;
    private String remark;
    private List<String> attachmentUrls;
    private List<BillShareItemCommand> shareItems;
}
