package com.spvermicelli.tripledger.billing.domain.request.repository;

import com.spvermicelli.tripledger.billing.domain.request.model.BillChangeRequestApproval;
import java.util.List;

public interface BillChangeRequestApprovalRepository {

    BillChangeRequestApproval save(BillChangeRequestApproval approval);

    List<BillChangeRequestApproval> findByRequestId(Long requestId);
}
