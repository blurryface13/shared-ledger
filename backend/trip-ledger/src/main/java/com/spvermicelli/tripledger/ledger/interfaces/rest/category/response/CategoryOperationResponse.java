package com.spvermicelli.tripledger.ledger.interfaces.rest.category.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 分类写操作返回对象。
 * 单独保留 message 字段，便于在分类模块返回更贴近业务语义的提示语。
 */
@Getter
@Builder
public class CategoryOperationResponse {
    private Long categoryId;
    private String message;
}
