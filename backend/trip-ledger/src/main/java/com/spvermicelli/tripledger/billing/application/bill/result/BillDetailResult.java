package com.spvermicelli.tripledger.billing.application.bill.result;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BillDetailResult {

    private Long billId;
    private String billType;
    private String title;
    private Long billAmountCent;
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private LocalDateTime billTime;
    private String remark;
    private List<String> attachmentUrls;
    private String changeFlowStatus;
    private Long latestChangeRequestId;
    private boolean hasChangeHistory;
    private MemberSummaryResult payer;
    private MemberSummaryResult recorder;
    private TempSummaryResult carryTargetTempParticipant;
    private boolean directEditable;
    private boolean directDeletable;
    private Long viewerReceivableAmountCent;
    private Long viewerRecoveredAmountCent;
    private Long viewerUnrecoveredAmountCent;
    private Long viewerOwnShareAmountCent;
    private Long viewerOwnPaidAmountCent;
    private Long viewerOwnUnpaidAmountCent;
    private Long viewerAttachedTempShareAmountCent;
    private Long viewerAttachedTempRecoveredAmountCent;
    private Long viewerAttachedTempUnrecoveredAmountCent;
    private List<ParticipantResult> participants;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    public static class MemberSummaryResult {
        private Long memberId;
        private Long userId;
        private String nickname;
        private String avatarUrl;
        private String phoneNumber;
    }

    @Getter
    @Builder
    public static class TempSummaryResult {
        private Long tempParticipantId;
        private String nickname;
        private Long attachedMemberId;
        private String attachedMemberNickname;
    }

    @Getter
    @Builder
    public static class ParticipantResult {
        private String participantType;
        private Long participantRefId;
        private String nickname;
        private String avatarUrl;
        private Long attachedMemberId;
        private String attachedMemberNickname;
        private Long shareAmountCent;
        private Long confirmedAmountCent;
        private Long unconfirmedAmountCent;
    }
}
