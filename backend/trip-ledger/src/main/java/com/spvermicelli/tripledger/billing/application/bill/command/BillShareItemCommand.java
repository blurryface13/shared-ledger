package com.spvermicelli.tripledger.billing.application.bill.command;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

/**
 * 共享账单分摊项输入。
 * 这里只保留用例真正需要的字段，参与对象合法性与挂靠关系均由应用服务二次校验。
 */
@Getter
@Builder
public class BillShareItemCommand {

    private String participantType;
    private Long participantRefId;
    private String shareMethod;
    private BigDecimal shareRatio;
    private Long shareAmountCent;
}
