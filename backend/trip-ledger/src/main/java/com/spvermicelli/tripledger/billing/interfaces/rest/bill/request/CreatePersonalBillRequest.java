package com.spvermicelli.tripledger.billing.interfaces.rest.bill.request;

import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePersonalBillRequest {

    private String billType;

    @Size(max = 30, message = "title 长度不能超过30个字符")
    private String title;
    private Long billAmountCent;
    private Long categoryId;
    private Long payerMemberId;
    private Long recorderMemberId;
    private Long tempParticipantId;
    private LocalDateTime billTime;

    @Size(max = 500, message = "remark 长度不能超过500个字符")
    private String remark;
    private List<String> attachmentUrls;
}
