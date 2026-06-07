package com.spvermicelli.tripledger.identity.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spvermicelli.tripledger.shared.domain.enums.UserStatus;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.base.BaseAuditPO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("tb_user")
@EqualsAndHashCode(callSuper = true)
public class UserPO extends BaseAuditPO {

    @TableField("wechat_open_id")
    private String wechatOpenId;

    @TableField("wechat_union_id")
    private String wechatUnionId;

    @TableField("nickname")
    private String nickname;

    @TableField("avatar_url")
    private String avatarUrl;

    @TableField("mobile")
    private String mobile;

    @TableField("status")
    private UserStatus status;
}
