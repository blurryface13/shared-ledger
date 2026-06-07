package com.spvermicelli.tripledger.ledger.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spvermicelli.tripledger.shared.domain.enums.CategorySource;
import com.spvermicelli.tripledger.shared.domain.enums.CategoryStatus;
import com.spvermicelli.tripledger.shared.domain.enums.CategoryType;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.base.BaseAuditPO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("tb_book_category")
@EqualsAndHashCode(callSuper = true)
public class BookCategoryPO extends BaseAuditPO {
    @TableField("book_id")
    private Long bookId;
    @TableField("name")
    private String name;
    @TableField("icon")
    private String icon;
    @TableField("category_type")
    private CategoryType categoryType;
    @TableField("category_source")
    private CategorySource categorySource;
    @TableField("status")
    private CategoryStatus status;
    @TableField("sort_order")
    private Integer sortOrder;
    @TableField("created_by_user_id")
    private Long createdByUserId;
}
