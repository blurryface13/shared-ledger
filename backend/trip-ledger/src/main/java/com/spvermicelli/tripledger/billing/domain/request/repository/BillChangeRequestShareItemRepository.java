package com.spvermicelli.tripledger.billing.domain.request.repository;

import com.spvermicelli.tripledger.billing.domain.request.model.BillChangeRequestShareItem;
import java.util.List;

public interface BillChangeRequestShareItemRepository {

    void saveAll(Long requestId, List<BillChangeRequestShareItem> items);

    List<BillChangeRequestShareItem> findByRequestId(Long requestId);

    void deleteByRequestId(Long requestId);
}
