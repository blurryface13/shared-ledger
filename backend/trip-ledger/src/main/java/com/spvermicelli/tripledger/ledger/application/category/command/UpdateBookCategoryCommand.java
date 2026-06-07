package com.spvermicelli.tripledger.ledger.application.category.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateBookCategoryCommand {
    private Long currentUserId;
    private Long bookId;
    private Long categoryId;
    private String name;
    private String icon;
}
