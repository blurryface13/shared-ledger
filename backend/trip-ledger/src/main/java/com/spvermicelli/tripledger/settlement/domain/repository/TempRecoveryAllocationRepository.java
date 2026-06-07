package com.spvermicelli.tripledger.settlement.domain.repository;

import com.spvermicelli.tripledger.settlement.domain.model.TempRecoveryAllocation;
import java.util.Collection;
import java.util.List;

public interface TempRecoveryAllocationRepository {

    void saveAll(Long recoveryRecordId, List<TempRecoveryAllocation> allocations);

    void deleteByRecoveryRecordId(Long recoveryRecordId);

    List<TempRecoveryAllocation> findByRecoveryRecordId(Long recoveryRecordId);

    List<TempRecoveryAllocation> findByRecoveryRecordIds(Collection<Long> recoveryRecordIds);

    List<TempRecoveryAllocation> findByBillIds(Collection<Long> billIds);
}
