package com.spvermicelli.tripledger.billing.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spvermicelli.tripledger.billing.domain.request.model.BillChangeRequestApproval;
import com.spvermicelli.tripledger.billing.domain.request.repository.BillChangeRequestApprovalRepository;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.mapper.BillChangeRequestApprovalMapper;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.po.BillChangeRequestApprovalPO;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class BillChangeRequestApprovalRepositoryImpl implements BillChangeRequestApprovalRepository {

    private final BillChangeRequestApprovalMapper mapper;

    public BillChangeRequestApprovalRepositoryImpl(BillChangeRequestApprovalMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public BillChangeRequestApproval save(BillChangeRequestApproval approval) {
        BillChangeRequestApprovalPO po = toPO(approval);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public List<BillChangeRequestApproval> findByRequestId(Long requestId) {
        return mapper.selectList(new LambdaQueryWrapper<BillChangeRequestApprovalPO>()
                .eq(BillChangeRequestApprovalPO::getRequestId, requestId)
                .orderByAsc(BillChangeRequestApprovalPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    private BillChangeRequestApproval toDomain(BillChangeRequestApprovalPO po) {
        return BillChangeRequestApproval.builder()
            .id(po.getId())
            .requestId(po.getRequestId())
            .approverMemberId(po.getApproverMemberId())
            .approvalAction(po.getApprovalAction())
            .approvalComment(po.getApprovalComment())
            .createdAt(po.getCreatedAt())
            .build();
    }

    private BillChangeRequestApprovalPO toPO(BillChangeRequestApproval approval) {
        BillChangeRequestApprovalPO po = new BillChangeRequestApprovalPO();
        po.setId(approval.getId());
        po.setRequestId(approval.getRequestId());
        po.setApproverMemberId(approval.getApproverMemberId());
        po.setApprovalAction(approval.getApprovalAction());
        po.setApprovalComment(approval.getApprovalComment());
        po.setCreatedAt(approval.getCreatedAt());
        return po;
    }
}
