package com.spvermicelli.tripledger.settlement.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spvermicelli.tripledger.settlement.domain.model.TempRecoveryAllocation;
import com.spvermicelli.tripledger.settlement.domain.repository.TempRecoveryAllocationRepository;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.mapper.TempRecoveryAllocationMapper;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.po.TempRecoveryAllocationPO;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class TempRecoveryAllocationRepositoryImpl implements TempRecoveryAllocationRepository {

    private final TempRecoveryAllocationMapper mapper;

    public TempRecoveryAllocationRepositoryImpl(TempRecoveryAllocationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void saveAll(Long recoveryRecordId, List<TempRecoveryAllocation> allocations) {
        if (allocations == null || allocations.isEmpty()) {
            return;
        }
        for (TempRecoveryAllocation allocation : allocations) {
            TempRecoveryAllocationPO po = toPO(allocation);
            po.setRecoveryRecordId(recoveryRecordId);
            if (po.getId() == null) {
                mapper.insert(po);
            } else {
                mapper.updateById(po);
            }
        }
    }

    @Override
    public void deleteByRecoveryRecordId(Long recoveryRecordId) {
        mapper.delete(new LambdaQueryWrapper<TempRecoveryAllocationPO>()
            .eq(TempRecoveryAllocationPO::getRecoveryRecordId, recoveryRecordId));
    }

    @Override
    public List<TempRecoveryAllocation> findByRecoveryRecordId(Long recoveryRecordId) {
        return findByRecoveryRecordIds(Collections.singletonList(recoveryRecordId));
    }

    @Override
    public List<TempRecoveryAllocation> findByRecoveryRecordIds(Collection<Long> recoveryRecordIds) {
        if (recoveryRecordIds == null || recoveryRecordIds.isEmpty()) {
            return Collections.emptyList();
        }
        return mapper.selectList(new LambdaQueryWrapper<TempRecoveryAllocationPO>()
                .in(TempRecoveryAllocationPO::getRecoveryRecordId, recoveryRecordIds)
                .orderByAsc(TempRecoveryAllocationPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<TempRecoveryAllocation> findByBillIds(Collection<Long> billIds) {
        if (billIds == null || billIds.isEmpty()) {
            return Collections.emptyList();
        }
        return mapper.selectList(new LambdaQueryWrapper<TempRecoveryAllocationPO>()
                .in(TempRecoveryAllocationPO::getBillId, billIds)
                .orderByAsc(TempRecoveryAllocationPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    private TempRecoveryAllocation toDomain(TempRecoveryAllocationPO po) {
        return TempRecoveryAllocation.builder()
            .id(po.getId())
            .recoveryRecordId(po.getRecoveryRecordId())
            .billId(po.getBillId())
            .allocatedAmountCent(po.getAllocatedAmountCent())
            .createdAt(po.getCreatedAt())
            .build();
    }

    private TempRecoveryAllocationPO toPO(TempRecoveryAllocation allocation) {
        TempRecoveryAllocationPO po = new TempRecoveryAllocationPO();
        po.setId(allocation.getId());
        po.setRecoveryRecordId(allocation.getRecoveryRecordId());
        po.setBillId(allocation.getBillId());
        po.setAllocatedAmountCent(allocation.getAllocatedAmountCent());
        po.setCreatedAt(allocation.getCreatedAt());
        return po;
    }
}
