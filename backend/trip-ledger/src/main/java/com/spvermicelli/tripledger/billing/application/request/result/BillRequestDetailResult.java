package com.spvermicelli.tripledger.billing.application.request.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BillRequestDetailResult {

    private Long requestId;
    private Long billId;
    private Long predecessorRequestId;
    private boolean baseline;
    private String requestType;
    private Long requesterMemberId;
    private String requestReason;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime handledAt;
    private boolean currentUserCanApprove;
    private boolean currentUserCanCancel;
    private SnapshotResult snapshot;
    private List<ApprovalRecordResult> approvalRecords;

    @Getter
    @Builder
    public static class SnapshotResult {
        private String billType;
        private String title;
        private Long billAmountCent;
        private Long categoryId;
        private String categoryName;
        private String categoryIcon;
        private Long payerMemberId;
        private String payerMemberName;
        private Long recorderMemberId;
        private String recorderMemberName;
        private Long tempParticipantId;
        private String tempParticipantNickname;
        private LocalDateTime billTime;
        private String remark;
        private List<String> attachmentUrls;
        private List<SnapshotShareItemResult> shareItems;
    }

    @Getter
    @Builder
    public static class SnapshotShareItemResult {
        private String participantType;
        private Long participantRefId;
        private String participantName;
        private Long attachedMemberId;
        private String attachedMemberName;
        private String shareMethod;
        private BigDecimal shareRatio;
        private Long shareAmountCent;
    }

    @Getter
    @Builder
    public static class ApprovalRecordResult {
        private Long approvalId;
        private Long approverMemberId;
        private String approvalAction;
        private String approvalComment;
        private LocalDateTime createdAt;
    }
}
