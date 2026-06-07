package com.spvermicelli.tripledger.ledger.interfaces.rest.invitation.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateInvitationRequest {
    @NotNull(message = "inviteeUserId 不能为空")
    private Long inviteeUserId;

    @Size(max = 50, message = "remark 长度不能超过50个字符")
    private String remark;
}
