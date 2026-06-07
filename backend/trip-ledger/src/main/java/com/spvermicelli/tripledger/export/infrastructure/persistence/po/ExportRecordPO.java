package com.spvermicelli.tripledger.export.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spvermicelli.tripledger.shared.domain.enums.ExportType;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.base.BaseCreateTimePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("tb_export_record")
@EqualsAndHashCode(callSuper = true)
public class ExportRecordPO extends BaseCreateTimePO {
    @TableField("book_id")
    private Long bookId;
    @TableField("operator_member_id")
    private Long operatorMemberId;
    @TableField("export_type")
    private ExportType exportType;
    @TableField("file_url")
    private String fileUrl;
}
