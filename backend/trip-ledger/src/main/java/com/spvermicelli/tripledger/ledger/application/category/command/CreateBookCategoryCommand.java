package com.spvermicelli.tripledger.ledger.application.category.command;

import com.spvermicelli.tripledger.shared.domain.enums.CategorySource;
import com.spvermicelli.tripledger.shared.domain.enums.CategoryType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateBookCategoryCommand {
    private Long currentUserId;
    private Long bookId;
    private String name;
    private String icon;
    private CategoryType categoryType;
    private CategorySource categorySource;
}
