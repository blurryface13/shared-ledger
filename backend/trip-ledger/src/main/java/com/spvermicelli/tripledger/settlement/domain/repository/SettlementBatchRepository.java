package com.spvermicelli.tripledger.settlement.domain.repository;

import com.spvermicelli.tripledger.settlement.domain.model.SettlementBatch;
import java.util.List;
import java.util.Optional;

public interface SettlementBatchRepository {

    SettlementBatch save(SettlementBatch batch);

    Optional<SettlementBatch> findById(Long batchId);

    List<SettlementBatch> findByBookIdAndInitiatorMemberId(Long bookId, Long initiatorMemberId);
}
