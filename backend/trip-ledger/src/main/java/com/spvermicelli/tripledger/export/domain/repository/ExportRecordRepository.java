package com.spvermicelli.tripledger.export.domain.repository;

import com.spvermicelli.tripledger.export.domain.model.ExportRecord;
import java.util.List;

public interface ExportRecordRepository {

    ExportRecord save(ExportRecord exportRecord);

    List<ExportRecord> findByBookIdAndOperatorMemberId(Long bookId, Long operatorMemberId);
}
