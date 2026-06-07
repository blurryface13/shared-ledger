package com.spvermicelli.tripledger.settlement.domain.repository;

import com.spvermicelli.tripledger.settlement.domain.model.PaymentConfirmAllocation;
import java.util.Collection;
import java.util.List;

public interface PaymentConfirmAllocationRepository {

    void saveAll(Long paymentConfirmId, List<PaymentConfirmAllocation> allocations);

    void deleteByPaymentConfirmId(Long paymentConfirmId);

    List<PaymentConfirmAllocation> findByPaymentConfirmId(Long paymentConfirmId);

    List<PaymentConfirmAllocation> findByPaymentConfirmIds(Collection<Long> paymentConfirmIds);

    List<PaymentConfirmAllocation> findByBillIds(Collection<Long> billIds);
}
