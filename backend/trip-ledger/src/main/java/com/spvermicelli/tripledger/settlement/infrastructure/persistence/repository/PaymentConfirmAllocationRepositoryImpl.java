package com.spvermicelli.tripledger.settlement.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spvermicelli.tripledger.settlement.domain.model.PaymentConfirmAllocation;
import com.spvermicelli.tripledger.settlement.domain.repository.PaymentConfirmAllocationRepository;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.mapper.PaymentConfirmAllocationMapper;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.po.PaymentConfirmAllocationPO;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentConfirmAllocationRepositoryImpl implements PaymentConfirmAllocationRepository {

    private final PaymentConfirmAllocationMapper mapper;

    public PaymentConfirmAllocationRepositoryImpl(PaymentConfirmAllocationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void saveAll(Long paymentConfirmId, List<PaymentConfirmAllocation> allocations) {
        if (allocations == null || allocations.isEmpty()) {
            return;
        }
        for (PaymentConfirmAllocation allocation : allocations) {
            PaymentConfirmAllocationPO po = toPO(allocation);
            po.setPaymentConfirmId(paymentConfirmId);
            if (po.getId() == null) {
                mapper.insert(po);
            } else {
                mapper.updateById(po);
            }
        }
    }

    @Override
    public void deleteByPaymentConfirmId(Long paymentConfirmId) {
        mapper.delete(new LambdaQueryWrapper<PaymentConfirmAllocationPO>()
            .eq(PaymentConfirmAllocationPO::getPaymentConfirmId, paymentConfirmId));
    }

    @Override
    public List<PaymentConfirmAllocation> findByPaymentConfirmId(Long paymentConfirmId) {
        return findByPaymentConfirmIds(Collections.singletonList(paymentConfirmId));
    }

    @Override
    public List<PaymentConfirmAllocation> findByPaymentConfirmIds(Collection<Long> paymentConfirmIds) {
        if (paymentConfirmIds == null || paymentConfirmIds.isEmpty()) {
            return Collections.emptyList();
        }
        return mapper.selectList(new LambdaQueryWrapper<PaymentConfirmAllocationPO>()
                .in(PaymentConfirmAllocationPO::getPaymentConfirmId, paymentConfirmIds)
                .orderByAsc(PaymentConfirmAllocationPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<PaymentConfirmAllocation> findByBillIds(Collection<Long> billIds) {
        if (billIds == null || billIds.isEmpty()) {
            return Collections.emptyList();
        }
        return mapper.selectList(new LambdaQueryWrapper<PaymentConfirmAllocationPO>()
                .in(PaymentConfirmAllocationPO::getBillId, billIds)
                .orderByAsc(PaymentConfirmAllocationPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    private PaymentConfirmAllocation toDomain(PaymentConfirmAllocationPO po) {
        return PaymentConfirmAllocation.builder()
            .id(po.getId())
            .paymentConfirmId(po.getPaymentConfirmId())
            .billId(po.getBillId())
            .allocatedAmountCent(po.getAllocatedAmountCent())
            .createdAt(po.getCreatedAt())
            .build();
    }

    private PaymentConfirmAllocationPO toPO(PaymentConfirmAllocation allocation) {
        PaymentConfirmAllocationPO po = new PaymentConfirmAllocationPO();
        po.setId(allocation.getId());
        po.setPaymentConfirmId(allocation.getPaymentConfirmId());
        po.setBillId(allocation.getBillId());
        po.setAllocatedAmountCent(allocation.getAllocatedAmountCent());
        po.setCreatedAt(allocation.getCreatedAt());
        return po;
    }
}
