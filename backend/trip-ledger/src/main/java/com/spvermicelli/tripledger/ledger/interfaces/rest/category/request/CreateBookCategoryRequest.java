package com.spvermicelli.tripledger.ledger.interfaces.rest.category.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建分类请求。
 * 前端只负责传递分类基础资料，真正的作用域归属、排序值和权限均以后端为准。
 */
@Getter
@Setter
public class CreateBookCategoryRequest {
    @NotBlank(message = "name 不能为空")
    @Size(max = 12, message = "name 长度不能超过12个字符")
    private String name;
    private String icon;
    private String categoryType;
    private String categorySource;
}
