package com.spvermicelli.tripledger.ledger.application.category.result;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookCategoryResult {
    private Long categoryId;
    private Long bookId;
    private String name;
    private String icon;
    private String categoryType;
    private String categorySource;
    private String status;
    private Integer sortOrder;
    private Long createdByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
