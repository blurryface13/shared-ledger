package com.spvermicelli.tripledger.settlement.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spvermicelli.tripledger.settlement.domain.model.SettlementTransfer;
import com.spvermicelli.tripledger.settlement.domain.repository.SettlementTransferRepository;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.mapper.SettlementTransferMapper;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.po.SettlementTransferPO;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class SettlementTransferRepositoryImpl implements SettlementTransferRepository {

    private final SettlementTransferMapper mapper;

    public SettlementTransferRepositoryImpl(SettlementTransferMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void saveAll(List<SettlementTransfer> transfers) {
        if (transfers == null || transfers.isEmpty()) {
            return;
        }
        for (SettlementTransfer transfer : transfers) {
            SettlementTransferPO po = toPO(transfer);
            if (po.getId() == null) {
                mapper.insert(po);
            } else {
                mapper.updateById(po);
            }
        }
    }

    @Override
    public List<SettlementTransfer> findBySettlementBatchId(Long settlementBatchId) {
        return mapper.selectList(new LambdaQueryWrapper<SettlementTransferPO>()
                .eq(SettlementTransferPO::getSettlementBatchId, settlementBatchId)
                .orderByAsc(SettlementTransferPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public Optional<SettlementTransfer> findById(Long transferId) {
        return Optional.ofNullable(mapper.selectById(transferId)).map(this::toDomain);
    }

    private SettlementTransfer toDomain(SettlementTransferPO po) {
        return SettlementTransfer.builder()
            .id(po.getId())
            .settlementBatchId(po.getSettlementBatchId())
            .fromMemberId(po.getFromMemberId())
            .toMemberId(po.getToMemberId())
            .transferAmountCent(po.getTransferAmountCent())
            .relatedSummaryJson(po.getRelatedSummaryJson())
            .createdAt(po.getCreatedAt())
            .build();
    }

    private SettlementTransferPO toPO(SettlementTransfer transfer) {
        SettlementTransferPO po = new SettlementTransferPO();
        po.setId(transfer.getId());
        po.setSettlementBatchId(transfer.getSettlementBatchId());
        po.setFromMemberId(transfer.getFromMemberId());
        po.setToMemberId(transfer.getToMemberId());
        po.setTransferAmountCent(transfer.getTransferAmountCent());
        po.setRelatedSummaryJson(transfer.getRelatedSummaryJson());
        po.setCreatedAt(transfer.getCreatedAt());
        return po;
    }
}
