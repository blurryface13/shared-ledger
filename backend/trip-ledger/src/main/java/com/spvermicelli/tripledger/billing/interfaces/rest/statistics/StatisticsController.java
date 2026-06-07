package com.spvermicelli.tripledger.billing.interfaces.rest.statistics;

import com.spvermicelli.tripledger.billing.application.statistics.StatisticsApplicationService;
import com.spvermicelli.tripledger.billing.application.statistics.result.AttachedTempDetailResult;
import com.spvermicelli.tripledger.billing.application.statistics.result.CategoryConsumptionResult;
import com.spvermicelli.tripledger.billing.application.statistics.result.MemberRelationBillDetailResult;
import com.spvermicelli.tripledger.billing.application.statistics.result.MemberRelationResult;
import com.spvermicelli.tripledger.billing.application.statistics.result.StatisticsOverviewResult;
import com.spvermicelli.tripledger.billing.interfaces.rest.statistics.request.UpdateBudgetRequest;
import com.spvermicelli.tripledger.billing.interfaces.rest.statistics.response.AttachedTempDetailResponse;
import com.spvermicelli.tripledger.billing.interfaces.rest.statistics.response.BudgetOperationResponse;
import com.spvermicelli.tripledger.billing.interfaces.rest.statistics.response.CategoryConsumptionResponse;
import com.spvermicelli.tripledger.billing.interfaces.rest.statistics.response.MemberRelationBillDetailResponse;
import com.spvermicelli.tripledger.billing.interfaces.rest.statistics.response.MemberRelationResponse;
import com.spvermicelli.tripledger.billing.interfaces.rest.statistics.response.StatisticsOverviewResponse;
import com.spvermicelli.tripledger.shared.common.context.UserContextHolder;
import com.spvermicelli.tripledger.shared.common.response.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/books/{bookId}/statistics")
public class StatisticsController {

    private final StatisticsApplicationService statisticsApplicationService;

    public StatisticsController(StatisticsApplicationService statisticsApplicationService) {
        this.statisticsApplicationService = statisticsApplicationService;
    }

    @GetMapping("/overview")
    public ApiResponse<StatisticsOverviewResponse> getOverview(@PathVariable Long bookId) {
        StatisticsOverviewResult result = statisticsApplicationService.getOverview(UserContextHolder.getUserId(), bookId);
        return ApiResponse.success(StatisticsOverviewResponse.builder()
            .payFlowAmountCent(result.getPayFlowAmountCent())
            .accruedConsumptionAmountCent(result.getAccruedConsumptionAmountCent())
            .receivableAmountCent(result.getReceivableAmountCent())
            .pendingContributionAmountCent(result.getPendingContributionAmountCent())
            .personalIncomeAmountCent(result.getPersonalIncomeAmountCent())
            .recoveredFlowAmountCent(result.getRecoveredFlowAmountCent())
            .settledConsumptionAmountCent(result.getSettledConsumptionAmountCent())
            .currentMemberBudgetAmountCent(result.getCurrentMemberBudgetAmountCent())
            .budgetUsagePercent(result.getBudgetUsagePercent())
            .build());
    }

    @PutMapping("/budget")
    public ApiResponse<BudgetOperationResponse> updateBudget(
        @PathVariable Long bookId,
        @RequestBody(required = false) UpdateBudgetRequest request
    ) {
        Long budgetAmountCent = request == null ? 0L : request.getBudgetAmountCent();
        Long updated = statisticsApplicationService.updateCurrentMemberBudget(UserContextHolder.getUserId(), bookId, budgetAmountCent);
        return ApiResponse.success(BudgetOperationResponse.builder()
            .budgetAmountCent(updated)
            .message("预算设置成功")
            .build());
    }

    @GetMapping("/category-consumption")
    public ApiResponse<List<CategoryConsumptionResponse>> getCategoryConsumption(@PathVariable Long bookId) {
        return ApiResponse.success(statisticsApplicationService.getCategoryConsumption(UserContextHolder.getUserId(), bookId).stream()
            .map(this::toCategoryResponse)
            .toList());
    }

    @GetMapping("/member-relations")
    public ApiResponse<List<MemberRelationResponse>> getMemberRelations(@PathVariable Long bookId) {
        return ApiResponse.success(statisticsApplicationService.getMemberRelations(UserContextHolder.getUserId(), bookId).stream()
            .map(this::toMemberRelationResponse)
            .toList());
    }

    @GetMapping("/member-relations/details")
    public ApiResponse<List<MemberRelationBillDetailResponse>> getMemberRelationBillDetails(
        @PathVariable Long bookId,
        @RequestParam String direction,
        @RequestParam String targetType,
        @RequestParam Long targetId
    ) {
        return ApiResponse.success(statisticsApplicationService.getMemberRelationBillDetails(
                UserContextHolder.getUserId(),
                bookId,
                direction,
                targetType,
                targetId
            ).stream()
            .map(this::toMemberRelationBillDetailResponse)
            .toList());
    }

    @GetMapping("/attached-temp-details")
    public ApiResponse<List<AttachedTempDetailResponse>> getAttachedTempDetails(@PathVariable Long bookId) {
        return ApiResponse.success(statisticsApplicationService.getAttachedTempDetails(UserContextHolder.getUserId(), bookId).stream()
            .map(this::toAttachedTempDetailResponse)
            .toList());
    }

    private CategoryConsumptionResponse toCategoryResponse(CategoryConsumptionResult result) {
        return CategoryConsumptionResponse.builder()
            .categoryId(result.getCategoryId())
            .categoryName(result.getCategoryName())
            .categoryIcon(result.getCategoryIcon())
            .categoryType(result.getCategoryType())
            .consumptionAmountCent(result.getConsumptionAmountCent())
            .build();
    }

    private MemberRelationResponse toMemberRelationResponse(MemberRelationResult result) {
        return MemberRelationResponse.builder()
            .targetParticipantType(result.getTargetParticipantType())
            .targetParticipantId(result.getTargetParticipantId())
            .targetMemberId(result.getTargetMemberId())
            .targetMemberName(result.getTargetMemberName())
            .targetMemberAvatarUrl(result.getTargetMemberAvatarUrl())
            .iOweTargetAmountCent(result.getIOweTargetAmountCent())
            .iPaidTargetAmountCent(result.getIPaidTargetAmountCent())
            .iStillNeedPayTargetAmountCent(result.getIStillNeedPayTargetAmountCent())
            .iOweTargetTempAmountCent(result.getIOweTargetTempAmountCent())
            .targetOwesMeAmountCent(result.getTargetOwesMeAmountCent())
            .targetPaidMeAmountCent(result.getTargetPaidMeAmountCent())
            .targetStillNeedPayMeAmountCent(result.getTargetStillNeedPayMeAmountCent())
            .targetOwesMeTempAmountCent(result.getTargetOwesMeTempAmountCent())
            .netDirection(result.getNetDirection())
            .netAmountCent(result.getNetAmountCent())
            .build();
    }

    private MemberRelationBillDetailResponse toMemberRelationBillDetailResponse(MemberRelationBillDetailResult result) {
        return MemberRelationBillDetailResponse.builder()
            .billId(result.getBillId())
            .billType(result.getBillType())
            .title(result.getTitle())
            .remark(result.getRemark())
            .categoryId(result.getCategoryId())
            .categoryName(result.getCategoryName())
            .categoryIcon(result.getCategoryIcon())
            .billTime(result.getBillTime())
            .relationTag(result.getRelationTag())
            .amountCent(result.getAmountCent())
            .paidAmountCent(result.getPaidAmountCent())
            .unpaidAmountCent(result.getUnpaidAmountCent())
            .settled(result.isSettled())
            .tempIncluded(result.isTempIncluded())
            .build();
    }

    private AttachedTempDetailResponse toAttachedTempDetailResponse(AttachedTempDetailResult result) {
        return AttachedTempDetailResponse.builder()
            .tempParticipantId(result.getTempParticipantId())
            .tempParticipantNickname(result.getTempParticipantNickname())
            .tempParticipantType(result.getTempParticipantType())
            .totalReceivableAmountCent(result.getTotalReceivableAmountCent())
            .sharedExpenseReceivableAmountCent(result.getSharedExpenseReceivableAmountCent())
            .personalCarryReceivableAmountCent(result.getPersonalCarryReceivableAmountCent())
            .recoveredAmountCent(result.getRecoveredAmountCent())
            .unrecoveredAmountCent(result.getUnrecoveredAmountCent())
            .build();
    }
}
