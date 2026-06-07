package com.spvermicelli.tripledger.billing.interfaces.rest.statistics.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBudgetRequest {
    private Long budgetAmountCent;
}
