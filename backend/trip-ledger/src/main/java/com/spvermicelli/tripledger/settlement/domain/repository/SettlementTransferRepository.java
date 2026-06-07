package com.spvermicelli.tripledger.settlement.domain.repository;

import com.spvermicelli.tripledger.settlement.domain.model.SettlementTransfer;
import java.util.List;
import java.util.Optional;

public interface SettlementTransferRepository {

    void saveAll(List<SettlementTransfer> transfers);

    List<SettlementTransfer> findBySettlementBatchId(Long settlementBatchId);

    Optional<SettlementTransfer> findById(Long transferId);
}
