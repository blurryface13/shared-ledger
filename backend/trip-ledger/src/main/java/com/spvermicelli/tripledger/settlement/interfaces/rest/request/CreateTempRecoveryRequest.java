package com.spvermicelli.tripledger.settlement.interfaces.rest.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTempRecoveryRequest {
    @NotNull(message = "tempParticipantId 不能为空")
    private Long tempParticipantId;
    @NotNull(message = "recoveryAmountCent 不能为空")
    private Long recoveryAmountCent;

    @Size(max = 50, message = "remark 长度不能超过50个字符")
    private String remark;
}
