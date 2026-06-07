package com.spvermicelli.tripledger.billing.domain.bill.repository;

import com.spvermicelli.tripledger.billing.domain.bill.model.Bill;
import java.util.List;
import java.util.Optional;

public interface BillRepository {

    Bill save(Bill bill);

    Optional<Bill> findById(Long billId);

    List<Bill> findActiveByBookId(Long bookId);
}
