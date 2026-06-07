package com.spvermicelli.tripledger.billing.interfaces.rest.statistics.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberRelationBillDetailResponse {
    private Long billId;
    private String billType;
    private String title;
    private String remark;
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private LocalDateTime billTime;
    private String relationTag;
    private Long amountCent;
    private Long paidAmountCent;
    private Long unpaidAmountCent;
    private boolean settled;
    private boolean tempIncluded;
}
