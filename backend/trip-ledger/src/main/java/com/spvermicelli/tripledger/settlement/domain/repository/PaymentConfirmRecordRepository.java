package com.spvermicelli.tripledger.settlement.domain.repository;

import com.spvermicelli.tripledger.settlement.domain.model.PaymentConfirmRecord;
import com.spvermicelli.tripledger.shared.domain.enums.PaymentConfirmStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentConfirmRecordRepository {

    PaymentConfirmRecord save(PaymentConfirmRecord record);

    Optional<PaymentConfirmRecord> findById(Long recordId);

    List<PaymentConfirmRecord> findByBookId(Long bookId);

    List<PaymentConfirmRecord> findByBookIdAndToMemberIdAndStatus(Long bookId, Long toMemberId, PaymentConfirmStatus status);

    List<PaymentConfirmRecord> findByBookIdAndFromMemberIdAndStatus(Long bookId, Long fromMemberId, PaymentConfirmStatus status);

    List<PaymentConfirmRecord> findPendingNeedAutoConfirm(LocalDateTime deadline);
}
