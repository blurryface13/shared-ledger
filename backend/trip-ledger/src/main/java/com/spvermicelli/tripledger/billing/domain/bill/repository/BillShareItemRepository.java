package com.spvermicelli.tripledger.billing.domain.bill.repository;

import com.spvermicelli.tripledger.billing.domain.bill.model.BillShareItem;
import java.util.Collection;
import java.util.List;

public interface BillShareItemRepository {

    void saveAll(Long billId, List<BillShareItem> items);

    void deleteByBillId(Long billId);

    List<BillShareItem> findByBillId(Long billId);

    List<BillShareItem> findByBillIds(Collection<Long> billIds);
}
