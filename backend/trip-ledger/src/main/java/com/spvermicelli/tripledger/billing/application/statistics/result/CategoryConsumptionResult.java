package com.spvermicelli.tripledger.billing.application.statistics.result;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryConsumptionResult {
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryType;
    private Long consumptionAmountCent;
}
