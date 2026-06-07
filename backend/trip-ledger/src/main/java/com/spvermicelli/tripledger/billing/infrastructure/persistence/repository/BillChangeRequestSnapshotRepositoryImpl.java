package com.spvermicelli.tripledger.billing.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spvermicelli.tripledger.billing.domain.request.model.BillChangeRequestSnapshot;
import com.spvermicelli.tripledger.billing.domain.request.repository.BillChangeRequestSnapshotRepository;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.mapper.BillChangeRequestSnapshotMapper;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.po.BillChangeRequestSnapshotPO;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class BillChangeRequestSnapshotRepositoryImpl implements BillChangeRequestSnapshotRepository {

    private final BillChangeRequestSnapshotMapper mapper;

    public BillChangeRequestSnapshotRepositoryImpl(BillChangeRequestSnapshotMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public BillChangeRequestSnapshot save(BillChangeRequestSnapshot snapshot) {
        BillChangeRequestSnapshotPO po = toPO(snapshot);
        BillChangeRequestSnapshotPO existing = mapper.selectOne(new LambdaQueryWrapper<BillChangeRequestSnapshotPO>()
            .eq(BillChangeRequestSnapshotPO::getRequestId, snapshot.getRequestId())
            .last("limit 1"));
        if (existing == null) {
            mapper.insert(po);
        } else {
            po.setId(existing.getId());
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<BillChangeRequestSnapshot> findByRequestId(Long requestId) {
        if (requestId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<BillChangeRequestSnapshotPO>()
            .eq(BillChangeRequestSnapshotPO::getRequestId, requestId)
            .last("limit 1"))).map(this::toDomain);
    }

    private BillChangeRequestSnapshot toDomain(BillChangeRequestSnapshotPO po) {
        if (po == null) {
            return null;
        }
        return BillChangeRequestSnapshot.builder()
            .id(po.getId())
            .requestId(po.getRequestId())
            .billType(po.getBillType())
            .title(po.getTitle())
            .billAmountCent(po.getBillAmountCent())
            .categoryId(po.getCategoryId())
            .payerMemberId(po.getPayerMemberId())
            .recorderMemberId(po.getRecorderMemberId())
            .tempParticipantId(po.getTempParticipantId())
            .billTime(po.getBillTime())
            .remark(po.getRemark())
            .build();
    }

    private BillChangeRequestSnapshotPO toPO(BillChangeRequestSnapshot snapshot) {
        BillChangeRequestSnapshotPO po = new BillChangeRequestSnapshotPO();
        po.setId(snapshot.getId());
        po.setRequestId(snapshot.getRequestId());
        po.setBillType(snapshot.getBillType());
        po.setTitle(snapshot.getTitle());
        po.setBillAmountCent(snapshot.getBillAmountCent());
        po.setCategoryId(snapshot.getCategoryId());
        po.setPayerMemberId(snapshot.getPayerMemberId());
        po.setRecorderMemberId(snapshot.getRecorderMemberId());
        po.setTempParticipantId(snapshot.getTempParticipantId());
        po.setBillTime(snapshot.getBillTime());
        po.setRemark(snapshot.getRemark());
        return po;
    }
}
