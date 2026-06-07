package com.spvermicelli.tripledger.ledger.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spvermicelli.tripledger.shared.domain.enums.BookStatus;
import com.spvermicelli.tripledger.shared.domain.enums.BookType;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.base.BaseAuditPO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("tb_book")
@EqualsAndHashCode(callSuper = true)
public class BookPO extends BaseAuditPO {
    @TableField("name")
    private String name;
    @TableField("book_type")
    private BookType bookType;
    @TableField("owner_user_id")
    private Long ownerUserId;
    @TableField("description")
    private String description;
    @TableField("cover_url")
    private String coverUrl;
    @TableField("status")
    private BookStatus status;
}
