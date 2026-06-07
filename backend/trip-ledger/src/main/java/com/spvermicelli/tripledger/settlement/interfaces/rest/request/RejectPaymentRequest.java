package com.spvermicelli.tripledger.settlement.interfaces.rest.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectPaymentRequest {
    @Size(max = 50, message = "rejectRemark 长度不能超过50个字符")
    private String rejectRemark;
}
