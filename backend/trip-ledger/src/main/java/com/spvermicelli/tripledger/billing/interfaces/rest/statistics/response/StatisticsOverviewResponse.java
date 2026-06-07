package com.spvermicelli.tripledger.billing.interfaces.rest.statistics.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StatisticsOverviewResponse {
    private Long payFlowAmountCent;
    private Long accruedConsumptionAmountCent;
    private Long receivableAmountCent;
    private Long pendingContributionAmountCent;
    private Long personalIncomeAmountCent;
    private Long recoveredFlowAmountCent;
    private Long settledConsumptionAmountCent;
    private Long currentMemberBudgetAmountCent;
    private Double budgetUsagePercent;
}
