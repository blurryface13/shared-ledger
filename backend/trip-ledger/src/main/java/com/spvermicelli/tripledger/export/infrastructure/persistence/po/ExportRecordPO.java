package com.spvermicelli.tripledger.export.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spvermicelli.tripledger.shared.domain.enums.ExportStatus;
import com.spvermicelli.tripledger.shared.domain.enums.ExportType;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.base.BaseCreateTimePO;
import java.time.LocalDateTime;
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
    @TableField("export_status")
    private ExportStatus exportStatus;
    @TableField("file_url")
    private String fileUrl;
    @TableField("export_content_json")
    private String exportContentJson;
    @TableField("error_message")
    private String errorMessage;
    @TableField("started_at")
    private LocalDateTime startedAt;
    @TableField("finished_at")
    private LocalDateTime finishedAt;
}
