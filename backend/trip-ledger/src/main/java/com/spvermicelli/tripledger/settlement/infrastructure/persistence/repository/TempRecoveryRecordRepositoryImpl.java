package com.spvermicelli.tripledger.settlement.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spvermicelli.tripledger.settlement.domain.model.TempRecoveryRecord;
import com.spvermicelli.tripledger.settlement.domain.repository.TempRecoveryRecordRepository;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.mapper.TempRecoveryRecordMapper;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.po.TempRecoveryRecordPO;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class TempRecoveryRecordRepositoryImpl implements TempRecoveryRecordRepository {

    private final TempRecoveryRecordMapper mapper;

    public TempRecoveryRecordRepositoryImpl(TempRecoveryRecordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public TempRecoveryRecord save(TempRecoveryRecord record) {
        TempRecoveryRecordPO po = toPO(record);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public List<TempRecoveryRecord> findByBookId(Long bookId) {
        return mapper.selectList(new LambdaQueryWrapper<TempRecoveryRecordPO>()
                .eq(TempRecoveryRecordPO::getBookId, bookId)
                .orderByDesc(TempRecoveryRecordPO::getConfirmedAt)
                .orderByDesc(TempRecoveryRecordPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<TempRecoveryRecord> findByBookIdAndAttachedMemberId(Long bookId, Long attachedMemberId) {
        return mapper.selectList(new LambdaQueryWrapper<TempRecoveryRecordPO>()
                .eq(TempRecoveryRecordPO::getBookId, bookId)
                .eq(TempRecoveryRecordPO::getAttachedMemberId, attachedMemberId)
                .orderByDesc(TempRecoveryRecordPO::getConfirmedAt)
                .orderByDesc(TempRecoveryRecordPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    private TempRecoveryRecord toDomain(TempRecoveryRecordPO po) {
        return TempRecoveryRecord.builder()
            .id(po.getId())
            .bookId(po.getBookId())
            .tempParticipantId(po.getTempParticipantId())
            .attachedMemberId(po.getAttachedMemberId())
            .recoveryAmountCent(po.getRecoveryAmountCent())
            .confirmStatus(po.getConfirmStatus())
            .confirmedByMemberId(po.getConfirmedByMemberId())
            .createdAt(po.getCreatedAt())
            .confirmedAt(po.getConfirmedAt())
            .remark(po.getRemark())
            .build();
    }

    private TempRecoveryRecordPO toPO(TempRecoveryRecord record) {
        TempRecoveryRecordPO po = new TempRecoveryRecordPO();
        po.setId(record.getId());
        po.setBookId(record.getBookId());
        po.setTempParticipantId(record.getTempParticipantId());
        po.setAttachedMemberId(record.getAttachedMemberId());
        po.setRecoveryAmountCent(record.getRecoveryAmountCent());
        po.setConfirmStatus(record.getConfirmStatus());
        po.setConfirmedByMemberId(record.getConfirmedByMemberId());
        po.setCreatedAt(record.getCreatedAt());
        po.setConfirmedAt(record.getConfirmedAt());
        po.setRemark(record.getRemark());
        return po;
    }
}
