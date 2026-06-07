package com.spvermicelli.tripledger.billing.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spvermicelli.tripledger.billing.domain.request.model.BillChangeRequestShareItem;
import com.spvermicelli.tripledger.billing.domain.request.repository.BillChangeRequestShareItemRepository;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.mapper.BillChangeRequestShareItemMapper;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.po.BillChangeRequestShareItemPO;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class BillChangeRequestShareItemRepositoryImpl implements BillChangeRequestShareItemRepository {

    private final BillChangeRequestShareItemMapper mapper;

    public BillChangeRequestShareItemRepositoryImpl(BillChangeRequestShareItemMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void saveAll(Long requestId, List<BillChangeRequestShareItem> items) {
        deleteByRequestId(requestId);
        if (requestId == null || items == null || items.isEmpty()) {
            return;
        }
        items.forEach(item -> {
            BillChangeRequestShareItemPO po = new BillChangeRequestShareItemPO();
            po.setRequestId(requestId);
            po.setParticipantType(item.getParticipantType());
            po.setParticipantRefId(item.getParticipantRefId());
            po.setAttachedMemberId(item.getAttachedMemberId());
            po.setShareMethod(item.getShareMethod());
            po.setShareRatio(item.getShareRatio());
            po.setShareAmountCent(item.getShareAmountCent());
            mapper.insert(po);
        });
    }

    @Override
    public List<BillChangeRequestShareItem> findByRequestId(Long requestId) {
        if (requestId == null) {
            return List.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<BillChangeRequestShareItemPO>()
                .eq(BillChangeRequestShareItemPO::getRequestId, requestId)
                .orderByAsc(BillChangeRequestShareItemPO::getId))
            .stream()
            .map(po -> BillChangeRequestShareItem.builder()
                .id(po.getId())
                .requestId(po.getRequestId())
                .participantType(po.getParticipantType())
                .participantRefId(po.getParticipantRefId())
                .attachedMemberId(po.getAttachedMemberId())
                .shareMethod(po.getShareMethod())
                .shareRatio(po.getShareRatio())
                .shareAmountCent(po.getShareAmountCent())
                .build())
            .toList();
    }

    @Override
    public void deleteByRequestId(Long requestId) {
        if (requestId == null) {
            return;
        }
        mapper.delete(new LambdaQueryWrapper<BillChangeRequestShareItemPO>()
            .eq(BillChangeRequestShareItemPO::getRequestId, requestId));
    }
}
