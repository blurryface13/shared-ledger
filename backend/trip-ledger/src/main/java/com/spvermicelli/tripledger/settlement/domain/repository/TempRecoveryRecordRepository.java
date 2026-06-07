package com.spvermicelli.tripledger.settlement.domain.repository;

import com.spvermicelli.tripledger.settlement.domain.model.TempRecoveryRecord;
import java.util.List;

public interface TempRecoveryRecordRepository {

    TempRecoveryRecord save(TempRecoveryRecord record);

    List<TempRecoveryRecord> findByBookId(Long bookId);

    List<TempRecoveryRecord> findByBookIdAndAttachedMemberId(Long bookId, Long attachedMemberId);
}
