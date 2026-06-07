package com.spvermicelli.tripledger.ledger.interfaces.rest.category.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 更新分类请求。
 * 当前只开放名称与图标更新，分类类型、分类来源与排序值都不允许通过接口变更。
 */
@Getter
@Setter
public class UpdateBookCategoryRequest {
    @Size(max = 12, message = "name 长度不能超过12个字符")
    private String name;
    private String icon;
}
