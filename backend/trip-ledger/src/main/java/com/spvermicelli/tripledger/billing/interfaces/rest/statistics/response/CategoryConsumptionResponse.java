package com.spvermicelli.tripledger.billing.interfaces.rest.statistics.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryConsumptionResponse {
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryType;
    private Long consumptionAmountCent;
}
