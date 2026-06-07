package com.spvermicelli.tripledger.ledger.application.category.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DisableBookCategoryCommand {
    private Long currentUserId;
    private Long bookId;
    private Long categoryId;
}
