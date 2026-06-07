package com.spvermicelli.tripledger.settlement.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spvermicelli.tripledger.settlement.domain.model.PaymentConfirmRecord;
import com.spvermicelli.tripledger.settlement.domain.repository.PaymentConfirmRecordRepository;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.mapper.PaymentConfirmRecordMapper;
import com.spvermicelli.tripledger.settlement.infrastructure.persistence.po.PaymentConfirmRecordPO;
import com.spvermicelli.tripledger.shared.domain.enums.PaymentConfirmStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentConfirmRecordRepositoryImpl implements PaymentConfirmRecordRepository {

    private final PaymentConfirmRecordMapper mapper;

    public PaymentConfirmRecordRepositoryImpl(PaymentConfirmRecordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PaymentConfirmRecord save(PaymentConfirmRecord record) {
        PaymentConfirmRecordPO po = toPO(record);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<PaymentConfirmRecord> findById(Long recordId) {
        return Optional.ofNullable(mapper.selectById(recordId)).map(this::toDomain);
    }

    @Override
    public List<PaymentConfirmRecord> findByBookId(Long bookId) {
        return mapper.selectList(new LambdaQueryWrapper<PaymentConfirmRecordPO>()
                .eq(PaymentConfirmRecordPO::getBookId, bookId)
                .orderByDesc(PaymentConfirmRecordPO::getInitiatedAt)
                .orderByDesc(PaymentConfirmRecordPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<PaymentConfirmRecord> findByBookIdAndToMemberIdAndStatus(
        Long bookId,
        Long toMemberId,
        PaymentConfirmStatus status
    ) {
        return mapper.selectList(new LambdaQueryWrapper<PaymentConfirmRecordPO>()
                .eq(PaymentConfirmRecordPO::getBookId, bookId)
                .eq(PaymentConfirmRecordPO::getToMemberId, toMemberId)
                .eq(PaymentConfirmRecordPO::getConfirmStatus, status)
                .orderByDesc(PaymentConfirmRecordPO::getInitiatedAt)
                .orderByDesc(PaymentConfirmRecordPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<PaymentConfirmRecord> findByBookIdAndFromMemberIdAndStatus(
        Long bookId,
        Long fromMemberId,
        PaymentConfirmStatus status
    ) {
        return mapper.selectList(new LambdaQueryWrapper<PaymentConfirmRecordPO>()
                .eq(PaymentConfirmRecordPO::getBookId, bookId)
                .eq(PaymentConfirmRecordPO::getFromMemberId, fromMemberId)
                .eq(PaymentConfirmRecordPO::getConfirmStatus, status)
                .orderByDesc(PaymentConfirmRecordPO::getInitiatedAt)
                .orderByDesc(PaymentConfirmRecordPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<PaymentConfirmRecord> findPendingNeedAutoConfirm(LocalDateTime deadline) {
        return mapper.selectList(new LambdaQueryWrapper<PaymentConfirmRecordPO>()
                .eq(PaymentConfirmRecordPO::getConfirmStatus, PaymentConfirmStatus.PENDING_CONFIRM)
                .lt(PaymentConfirmRecordPO::getInitiatedAt, deadline))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    private PaymentConfirmRecord toDomain(PaymentConfirmRecordPO po) {
        return PaymentConfirmRecord.builder()
            .id(po.getId())
            .bookId(po.getBookId())
            .fromMemberId(po.getFromMemberId())
            .toMemberId(po.getToMemberId())
            .paymentAmountCent(po.getPaymentAmountCent())
            .sourceType(po.getSourceType())
            .sourceRefId(po.getSourceRefId())
            .confirmStatus(po.getConfirmStatus())
            .initiatedByMemberId(po.getInitiatedByMemberId())
            .confirmedByMemberId(po.getConfirmedByMemberId())
            .initiatedAt(po.getInitiatedAt())
            .confirmedAt(po.getConfirmedAt())
            .autoConfirmAt(po.getAutoConfirmAt())
            .remark(po.getRemark())
            .build();
    }

    private PaymentConfirmRecordPO toPO(PaymentConfirmRecord record) {
        PaymentConfirmRecordPO po = new PaymentConfirmRecordPO();
        po.setId(record.getId());
        po.setBookId(record.getBookId());
        po.setFromMemberId(record.getFromMemberId());
        po.setToMemberId(record.getToMemberId());
        po.setPaymentAmountCent(record.getPaymentAmountCent());
        po.setSourceType(record.getSourceType());
        po.setSourceRefId(record.getSourceRefId());
        po.setConfirmStatus(record.getConfirmStatus());
        po.setInitiatedByMemberId(record.getInitiatedByMemberId());
        po.setConfirmedByMemberId(record.getConfirmedByMemberId());
        po.setInitiatedAt(record.getInitiatedAt());
        po.setConfirmedAt(record.getConfirmedAt());
        po.setAutoConfirmAt(record.getAutoConfirmAt());
        po.setRemark(record.getRemark());
        return po;
    }
}
