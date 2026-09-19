package com.spvermicelli.tripledger.export.domain.repository;

import com.spvermicelli.tripledger.export.domain.model.ExportRecord;
import java.util.List;
import java.util.Optional;

public interface ExportRecordRepository {

    ExportRecord save(ExportRecord exportRecord);

    Optional<ExportRecord> findById(Long exportRecordId);

    List<ExportRecord> findByBookIdAndOperatorMemberId(Long bookId, Long operatorMemberId);
}
