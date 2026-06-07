package com.spvermicelli.tripledger.billing.application.support;

import com.spvermicelli.tripledger.billing.domain.bill.model.Bill;
import com.spvermicelli.tripledger.billing.domain.bill.model.BillShareItem;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 当前用户可见账单聚合快照。
 * 账单主表与分摊项需要一起参与可见性、统计和结算计算，因此在应用层组合成一个只读聚合视图。
 */
@Getter
@Builder
public class VisibleBillAggregate {

    private Bill bill;
    private List<BillShareItem> shareItems;
}
