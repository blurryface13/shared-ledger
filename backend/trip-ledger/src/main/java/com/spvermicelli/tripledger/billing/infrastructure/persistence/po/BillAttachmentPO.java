package com.spvermicelli.tripledger.billing.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spvermicelli.tripledger.shared.domain.enums.AttachmentFileType;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.base.BaseCreateTimePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("tb_bill_attachment")
@EqualsAndHashCode(callSuper = true)
public class BillAttachmentPO extends BaseCreateTimePO {
    @TableField("bill_id")
    private Long billId;
    @TableField("file_url")
    private String fileUrl;
    @TableField("file_type")
    private AttachmentFileType fileType;
    @TableField("uploaded_by_member_id")
    private Long uploadedByMemberId;
}
