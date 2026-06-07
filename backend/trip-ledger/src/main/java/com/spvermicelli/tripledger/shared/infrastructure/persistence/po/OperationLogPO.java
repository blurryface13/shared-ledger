package com.spvermicelli.tripledger.shared.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spvermicelli.tripledger.shared.domain.enums.OperationTargetType;
import com.spvermicelli.tripledger.shared.domain.enums.OperationType;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.base.BaseCreateTimePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("tb_operation_log")
@EqualsAndHashCode(callSuper = true)
public class OperationLogPO extends BaseCreateTimePO {
    @TableField("book_id")
    private Long bookId;
    @TableField("operator_member_id")
    private Long operatorMemberId;
    @TableField("operator_user_id")
    private Long operatorUserId;
    @TableField("operation_type")
    private OperationType operationType;
    @TableField("target_type")
    private OperationTargetType targetType;
    @TableField("target_id")
    private Long targetId;
    @TableField("before_json")
    private String beforeJson;
    @TableField("after_json")
    private String afterJson;
}
