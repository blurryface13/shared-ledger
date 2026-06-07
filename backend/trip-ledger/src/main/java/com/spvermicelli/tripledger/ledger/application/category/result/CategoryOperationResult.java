package com.spvermicelli.tripledger.ledger.application.category.result;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryOperationResult {
    private Long categoryId;
    private String message;
}
