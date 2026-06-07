package com.spvermicelli.tripledger.billing.application.support;

import com.spvermicelli.tripledger.billing.domain.bill.model.Bill;
import com.spvermicelli.tripledger.billing.domain.bill.model.BillShareItem;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * 账本账单投影。
 * 统一聚合账单、分摊项、支付分配、临时成员回补分配等信息，
 * 让上层接口在同一份投影之上完成账单详情、统计、结算和导出计算。
 */
@Getter
@Builder
public class BillingProjection {

    private Map<Long, Bill> billMap;
    private Map<Long, List<BillShareItem>> shareItemsByBillId;
    private List<SharedDebtLine> sharedDebtLines;
    private List<TempRecoveryLine> tempRecoveryLines;
    private List<PaymentAllocationView> paymentAllocationViews;
    private List<TempRecoveryAllocationView> tempRecoveryAllocationViews;
}
