package com.spvermicelli.tripledger.billing.domain.bill.model;

import com.spvermicelli.tripledger.shared.domain.enums.BillStatus;
import com.spvermicelli.tripledger.shared.domain.enums.BillChangeFlowStatus;
import com.spvermicelli.tripledger.shared.domain.enums.BillType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

/**
 * 账单聚合根。
 * 账单主表承担账单基础身份、金额、分类、付款人与记账人等核心属性，
 * 分摊项、申请流、支付确认等围绕该聚合根展开。
 */
@Getter
@Builder
public class Bill {

    private Long id;
    private Long bookId;
    private BillType billType;
    private String title;
    private Long billAmountCent;
    private Long categoryId;
    private Long payerMemberId;
    private Long recorderMemberId;
    private Long targetTempParticipantId;
    private LocalDateTime billTime;
    private String remark;
    private BillChangeFlowStatus changeFlowStatus;
    private Long latestChangeRequestId;
    private boolean hasChangeHistory;
    private BillStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isActive() {
        return status == BillStatus.ACTIVE;
    }

    public boolean isSharedExpense() {
        return billType == BillType.SHARED_EXPENSE;
    }

    public boolean isPersonalCarry() {
        return billType == BillType.PERSONAL_CARRY;
    }

    public boolean isPersonalIncome() {
        return billType == BillType.PERSONAL_INCOME;
    }

    public boolean isPersonalExpense() {
        return billType == BillType.PERSONAL_EXPENSE;
    }

    /**
     * 生成更新后的账单快照。
     * 更新时始终保留原账单主键、创建时间和当前状态，避免对历史链路造成破坏。
     */
    public Bill update(
        BillType nextBillType,
        String nextTitle,
        Long nextBillAmountCent,
        Long nextCategoryId,
        Long nextPayerMemberId,
        Long nextRecorderMemberId,
        Long nextTargetTempParticipantId,
        LocalDateTime nextBillTime,
        String nextRemark
    ) {
        return Bill.builder()
            .id(id)
            .bookId(bookId)
            .billType(nextBillType)
            .title(StringUtils.hasText(nextTitle) ? nextTitle.trim() : title)
            .billAmountCent(nextBillAmountCent)
            .categoryId(nextCategoryId)
            .payerMemberId(nextPayerMemberId)
            .recorderMemberId(nextRecorderMemberId)
            .targetTempParticipantId(nextTargetTempParticipantId)
            .billTime(nextBillTime)
            .remark(normalizeRemark(nextRemark))
            .changeFlowStatus(changeFlowStatus)
            .latestChangeRequestId(latestChangeRequestId)
            .hasChangeHistory(hasChangeHistory)
            .status(status)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }

    public Bill delete() {
        return Bill.builder()
            .id(id)
            .bookId(bookId)
            .billType(billType)
            .title(title)
            .billAmountCent(billAmountCent)
            .categoryId(categoryId)
            .payerMemberId(payerMemberId)
            .recorderMemberId(recorderMemberId)
            .targetTempParticipantId(targetTempParticipantId)
            .billTime(billTime)
            .remark(remark)
            .changeFlowStatus(changeFlowStatus)
            .latestChangeRequestId(latestChangeRequestId)
            .hasChangeHistory(hasChangeHistory)
            .status(BillStatus.DELETED)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }

    private String normalizeRemark(String rawRemark) {
        return StringUtils.hasText(rawRemark) ? rawRemark.trim() : null;
    }
}
