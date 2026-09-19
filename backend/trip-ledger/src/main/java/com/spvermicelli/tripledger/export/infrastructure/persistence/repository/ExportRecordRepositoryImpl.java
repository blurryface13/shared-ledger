package com.spvermicelli.tripledger.export.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spvermicelli.tripledger.export.domain.model.ExportRecord;
import com.spvermicelli.tripledger.export.domain.repository.ExportRecordRepository;
import com.spvermicelli.tripledger.export.infrastructure.persistence.mapper.ExportRecordMapper;
import com.spvermicelli.tripledger.export.infrastructure.persistence.po.ExportRecordPO;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ExportRecordRepositoryImpl implements ExportRecordRepository {

    private final ExportRecordMapper mapper;

    public ExportRecordRepositoryImpl(ExportRecordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ExportRecord save(ExportRecord exportRecord) {
        ExportRecordPO po = toPO(exportRecord);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<ExportRecord> findById(Long exportRecordId) {
        return Optional.ofNullable(mapper.selectForUpdate(exportRecordId)).map(this::toDomain);
    }

    @Override
    public List<ExportRecord> findByBookIdAndOperatorMemberId(Long bookId, Long operatorMemberId) {
        return mapper.selectList(new LambdaQueryWrapper<ExportRecordPO>()
                .eq(ExportRecordPO::getBookId, bookId)
                .eq(ExportRecordPO::getOperatorMemberId, operatorMemberId)
                .orderByDesc(ExportRecordPO::getCreatedAt)
                .orderByDesc(ExportRecordPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    private ExportRecord toDomain(ExportRecordPO po) {
        return ExportRecord.builder()
            .id(po.getId())
            .bookId(po.getBookId())
            .operatorMemberId(po.getOperatorMemberId())
            .exportType(po.getExportType())
            .exportStatus(po.getExportStatus())
            .fileUrl(po.getFileUrl())
            .exportContentJson(po.getExportContentJson())
            .errorMessage(po.getErrorMessage())
            .createdAt(po.getCreatedAt())
            .startedAt(po.getStartedAt())
            .finishedAt(po.getFinishedAt())
            .build();
    }

    private ExportRecordPO toPO(ExportRecord domain) {
        ExportRecordPO po = new ExportRecordPO();
        po.setId(domain.getId());
        po.setBookId(domain.getBookId());
        po.setOperatorMemberId(domain.getOperatorMemberId());
        po.setExportType(domain.getExportType());
        po.setExportStatus(domain.getExportStatus());
        po.setFileUrl(domain.getFileUrl());
        po.setExportContentJson(domain.getExportContentJson());
        po.setErrorMessage(domain.getErrorMessage());
        po.setCreatedAt(domain.getCreatedAt());
        po.setStartedAt(domain.getStartedAt());
        po.setFinishedAt(domain.getFinishedAt());
        return po;
    }
}
