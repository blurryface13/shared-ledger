package com.spvermicelli.tripledger.billing.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spvermicelli.tripledger.shared.domain.enums.AttachmentFileType;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.base.BaseCreateTimePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("tb_bill_change_request_attachment")
@EqualsAndHashCode(callSuper = true)
public class BillChangeRequestAttachmentPO extends BaseCreateTimePO {
    @TableField("request_id")
    private Long requestId;
    @TableField("file_url")
    private String fileUrl;
    @TableField("file_type")
    private AttachmentFileType fileType;
    @TableField("uploaded_by_member_id")
    private Long uploadedByMemberId;
}
