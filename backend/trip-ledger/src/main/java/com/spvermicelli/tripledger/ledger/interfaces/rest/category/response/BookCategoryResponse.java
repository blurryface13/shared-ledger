package com.spvermicelli.tripledger.ledger.interfaces.rest.category.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 分类列表返回对象。
 * 直接面向前端返回当前账本可见的分类信息，便于账单创建页直接使用。
 */
@Getter
@Builder
public class BookCategoryResponse {
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
