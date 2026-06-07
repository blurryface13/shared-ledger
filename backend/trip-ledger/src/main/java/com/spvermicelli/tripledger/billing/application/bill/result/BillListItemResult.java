package com.spvermicelli.tripledger.billing.application.bill.result;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BillListItemResult {

    private Long billId;
    private String billType;
    private String title;
    private Long billAmountCent;
    private String remark;
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private LocalDateTime billTime;
    private LocalDateTime createdAt;
    private boolean viewerIsPayer;
    private boolean viewerIsRecorder;
    private boolean viewerHasAttachedTempShare;
    private boolean directEditable;
    private boolean directDeletable;
    private Long viewerOwnShareAmountCent;
    private Long viewerOwnPaidAmountCent;
    private Long viewerOwnUnpaidAmountCent;
    private Long viewerAttachedTempShareAmountCent;
    private Long viewerAttachedTempRecoveredAmountCent;
    private Long viewerAttachedTempUnrecoveredAmountCent;
    private Long viewerReceivableAmountCent;
    private Long viewerRecoveredAmountCent;
    private Long viewerUnrecoveredAmountCent;
}
