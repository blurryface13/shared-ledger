package com.spvermicelli.tripledger.billing.interfaces.rest.bill.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettleBillParticipantRequest {
    @Size(max = 50, message = "remark 长度不能超过50个字符")
    private String remark;
}
