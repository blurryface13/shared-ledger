package com.spvermicelli.tripledger.billing.domain.request.repository;

import com.spvermicelli.tripledger.billing.domain.request.model.BillChangeRequestSnapshot;
import java.util.Optional;

public interface BillChangeRequestSnapshotRepository {

    BillChangeRequestSnapshot save(BillChangeRequestSnapshot snapshot);

    Optional<BillChangeRequestSnapshot> findByRequestId(Long requestId);
}
