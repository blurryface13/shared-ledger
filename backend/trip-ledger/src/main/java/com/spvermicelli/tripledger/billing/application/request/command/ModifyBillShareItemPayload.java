package com.spvermicelli.tripledger.billing.application.request.command;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 账单修改申请里的分摊项快照。
 * 这里故意不直接复用应用层命令对象，避免 JSON 快照反序列化时依赖命令类型的构造方式。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifyBillShareItemPayload {

    private String participantType;
    private Long participantRefId;
    private String shareMethod;
    private BigDecimal shareRatio;
    private Long shareAmountCent;
}
