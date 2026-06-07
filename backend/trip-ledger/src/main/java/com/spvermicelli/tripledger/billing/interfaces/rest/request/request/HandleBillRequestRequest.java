package com.spvermicelli.tripledger.billing.interfaces.rest.request.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HandleBillRequestRequest {

    @Size(max = 200, message = "approvalComment 长度不能超过200个字符")
    private String approvalComment;
}
