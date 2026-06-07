package com.spvermicelli.tripledger.ledger.interfaces.rest.tempParticipant.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTempParticipantNicknameRequest {
    @NotBlank(message = "nickname 不能为空")
    @Size(max = 20, message = "nickname 长度不能超过20个字符")
    private String nickname;
}
