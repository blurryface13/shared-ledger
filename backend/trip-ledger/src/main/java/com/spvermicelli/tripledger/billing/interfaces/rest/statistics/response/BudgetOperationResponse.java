package com.spvermicelli.tripledger.billing.interfaces.rest.statistics.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BudgetOperationResponse {
    private Long budgetAmountCent;
    private String message;
}
