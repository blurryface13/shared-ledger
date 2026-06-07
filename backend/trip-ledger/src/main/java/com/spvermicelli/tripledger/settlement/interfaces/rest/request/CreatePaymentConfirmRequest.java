package com.spvermicelli.tripledger.settlement.interfaces.rest.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePaymentConfirmRequest {
    @NotNull(message = "toMemberId 不能为空")
    private Long toMemberId;
    @NotNull(message = "paymentAmountCent 不能为空")
    private Long paymentAmountCent;
    private String sourceType;
    private Long sourceRefId;

    @Size(max = 50, message = "remark 长度不能超过50个字符")
    private String remark;
}
