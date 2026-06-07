package com.spvermicelli.tripledger.billing.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spvermicelli.tripledger.billing.domain.bill.model.BillShareItem;
import com.spvermicelli.tripledger.billing.domain.bill.repository.BillShareItemRepository;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.mapper.BillShareItemMapper;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.po.BillShareItemPO;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class BillShareItemRepositoryImpl implements BillShareItemRepository {

    private final BillShareItemMapper billShareItemMapper;

    public BillShareItemRepositoryImpl(BillShareItemMapper billShareItemMapper) {
        this.billShareItemMapper = billShareItemMapper;
    }

    @Override
    public void saveAll(Long billId, List<BillShareItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (BillShareItem item : items) {
            BillShareItemPO po = toPO(item);
            po.setBillId(billId);
            if (po.getId() == null) {
                billShareItemMapper.insert(po);
            } else {
                billShareItemMapper.updateById(po);
            }
        }
    }

    @Override
    public void deleteByBillId(Long billId) {
        billShareItemMapper.delete(new LambdaQueryWrapper<BillShareItemPO>().eq(BillShareItemPO::getBillId, billId));
    }

    @Override
    public List<BillShareItem> findByBillId(Long billId) {
        return findByBillIds(Collections.singletonList(billId));
    }

    @Override
    public List<BillShareItem> findByBillIds(Collection<Long> billIds) {
        if (billIds == null || billIds.isEmpty()) {
            return Collections.emptyList();
        }
        return billShareItemMapper.selectList(new LambdaQueryWrapper<BillShareItemPO>()
                .in(BillShareItemPO::getBillId, billIds)
                .orderByAsc(BillShareItemPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    private BillShareItem toDomain(BillShareItemPO po) {
        return BillShareItem.builder()
            .id(po.getId())
            .billId(po.getBillId())
            .participantType(po.getParticipantType())
            .participantRefId(po.getParticipantRefId())
            .attachedMemberId(po.getAttachedMemberId())
            .shareMethod(po.getShareMethod())
            .shareRatio(po.getShareRatio())
            .shareAmountCent(po.getShareAmountCent())
            .createdAt(po.getCreatedAt())
            .build();
    }

    private BillShareItemPO toPO(BillShareItem item) {
        BillShareItemPO po = new BillShareItemPO();
        po.setId(item.getId());
        po.setBillId(item.getBillId());
        po.setParticipantType(item.getParticipantType());
        po.setParticipantRefId(item.getParticipantRefId());
        po.setAttachedMemberId(item.getAttachedMemberId());
        po.setShareMethod(item.getShareMethod());
        po.setShareRatio(item.getShareRatio());
        po.setShareAmountCent(item.getShareAmountCent());
        po.setCreatedAt(item.getCreatedAt());
        return po;
    }
}
