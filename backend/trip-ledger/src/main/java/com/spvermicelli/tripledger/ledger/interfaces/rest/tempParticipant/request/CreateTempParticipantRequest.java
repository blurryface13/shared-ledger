package com.spvermicelli.tripledger.ledger.interfaces.rest.tempParticipant.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建临时成员请求。
 * 真正的账本归属、创建人身份和权限以后端解析 token 与路径参数为准。
 */
@Getter
@Setter
public class CreateTempParticipantRequest {
    @NotBlank(message = "nickname 不能为空")
    @Size(max = 20, message = "nickname 长度不能超过20个字符")
    private String nickname;
    private String tempType;
    private Long attachedMemberId;
}
