package com.spvermicelli.tripledger.billing.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spvermicelli.tripledger.billing.domain.bill.model.Bill;
import com.spvermicelli.tripledger.billing.domain.bill.repository.BillRepository;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.mapper.BillMapper;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.po.BillPO;
import com.spvermicelli.tripledger.shared.domain.enums.BillStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class BillRepositoryImpl implements BillRepository {

    private final BillMapper billMapper;

    public BillRepositoryImpl(BillMapper billMapper) {
        this.billMapper = billMapper;
    }

    @Override
    public Bill save(Bill bill) {
        BillPO po = toPO(bill);
        if (po.getId() == null) {
            billMapper.insert(po);
        } else {
            billMapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<Bill> findById(Long billId) {
        return Optional.ofNullable(billMapper.selectById(billId)).map(this::toDomain);
    }

    @Override
    public List<Bill> findActiveByBookId(Long bookId) {
        return billMapper.selectList(new LambdaQueryWrapper<BillPO>()
                .eq(BillPO::getBookId, bookId)
                .eq(BillPO::getStatus, BillStatus.ACTIVE)
                .orderByDesc(BillPO::getBillTime)
                .orderByDesc(BillPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    private Bill toDomain(BillPO po) {
        return Bill.builder()
            .id(po.getId())
            .bookId(po.getBookId())
            .billType(po.getBillType())
            .title(po.getTitle())
            .billAmountCent(po.getBillAmountCent())
            .categoryId(po.getCategoryId())
            .payerMemberId(po.getPayerMemberId())
            .recorderMemberId(po.getRecorderMemberId())
            .targetTempParticipantId(po.getTargetTempParticipantId())
            .billTime(po.getBillTime())
            .remark(po.getRemark())
            .changeFlowStatus(po.getChangeFlowStatus())
            .latestChangeRequestId(po.getLatestChangeRequestId())
            .hasChangeHistory(Boolean.TRUE.equals(po.getHasChangeHistory()))
            .status(po.getStatus())
            .createdAt(po.getCreatedAt())
            .updatedAt(po.getUpdatedAt())
            .build();
    }

    private BillPO toPO(Bill bill) {
        BillPO po = new BillPO();
        po.setId(bill.getId());
        po.setBookId(bill.getBookId());
        po.setBillType(bill.getBillType());
        po.setTitle(bill.getTitle());
        po.setBillAmountCent(bill.getBillAmountCent());
        po.setCategoryId(bill.getCategoryId());
        po.setPayerMemberId(bill.getPayerMemberId());
        po.setRecorderMemberId(bill.getRecorderMemberId());
        po.setTargetTempParticipantId(bill.getTargetTempParticipantId());
        po.setBillTime(bill.getBillTime());
        po.setRemark(bill.getRemark());
        po.setChangeFlowStatus(bill.getChangeFlowStatus());
        po.setLatestChangeRequestId(bill.getLatestChangeRequestId());
        po.setHasChangeHistory(bill.isHasChangeHistory());
        po.setStatus(bill.getStatus());
        po.setCreatedAt(bill.getCreatedAt());
        po.setUpdatedAt(bill.getUpdatedAt());
        return po;
    }
}
