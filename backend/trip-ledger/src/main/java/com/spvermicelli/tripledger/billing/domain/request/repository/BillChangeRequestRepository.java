package com.spvermicelli.tripledger.billing.domain.request.repository;

import com.spvermicelli.tripledger.billing.domain.request.model.BillChangeRequest;
import com.spvermicelli.tripledger.shared.domain.enums.ChangeRequestStatus;
import java.util.List;
import java.util.Optional;

public interface BillChangeRequestRepository {

    BillChangeRequest save(BillChangeRequest request);

    Optional<BillChangeRequest> findById(Long requestId);

    List<BillChangeRequest> findByBillId(Long billId);

    List<BillChangeRequest> findByBookIdAndRequesterMemberId(Long bookId, Long requesterMemberId);

    List<BillChangeRequest> findByBookIdAndStatus(Long bookId, ChangeRequestStatus status);
}
