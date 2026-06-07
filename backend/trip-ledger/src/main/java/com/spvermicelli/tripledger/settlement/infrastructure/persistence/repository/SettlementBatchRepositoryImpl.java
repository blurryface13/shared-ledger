package com.spvermicelli.tripledger.settlement.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spvermicelli.tripledger.settlement.domain.model.SettlementBatch;
import com.spvermicelli.tripledger.settlement.domain.repository.SettlementBatchRepository;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.mapper.SettlementBatchMapper;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.po.SettlementBatchPO;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class SettlementBatchRepositoryImpl implements SettlementBatchRepository {

    private final SettlementBatchMapper mapper;

    public SettlementBatchRepositoryImpl(SettlementBatchMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public SettlementBatch save(SettlementBatch batch) {
        SettlementBatchPO po = toPO(batch);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<SettlementBatch> findById(Long batchId) {
        return Optional.ofNullable(mapper.selectById(batchId)).map(this::toDomain);
    }

    @Override
    public List<SettlementBatch> findByBookIdAndInitiatorMemberId(Long bookId, Long initiatorMemberId) {
        return mapper.selectList(new LambdaQueryWrapper<SettlementBatchPO>()
                .eq(SettlementBatchPO::getBookId, bookId)
                .eq(SettlementBatchPO::getInitiatorMemberId, initiatorMemberId)
                .orderByDesc(SettlementBatchPO::getCreatedAt)
                .orderByDesc(SettlementBatchPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    private SettlementBatch toDomain(SettlementBatchPO po) {
        return SettlementBatch.builder()
            .id(po.getId())
            .bookId(po.getBookId())
            .initiatorMemberId(po.getInitiatorMemberId())
            .strategyType(po.getStrategyType())
            .scopeType(po.getScopeType())
            .status(po.getStatus())
            .snapshotTime(po.getSnapshotTime())
            .createdAt(po.getCreatedAt())
            .build();
    }

    private SettlementBatchPO toPO(SettlementBatch batch) {
        SettlementBatchPO po = new SettlementBatchPO();
        po.setId(batch.getId());
        po.setBookId(batch.getBookId());
        po.setInitiatorMemberId(batch.getInitiatorMemberId());
        po.setStrategyType(batch.getStrategyType());
        po.setScopeType(batch.getScopeType());
        po.setStatus(batch.getStatus());
        po.setSnapshotTime(batch.getSnapshotTime());
        po.setCreatedAt(batch.getCreatedAt());
        return po;
    }
}
