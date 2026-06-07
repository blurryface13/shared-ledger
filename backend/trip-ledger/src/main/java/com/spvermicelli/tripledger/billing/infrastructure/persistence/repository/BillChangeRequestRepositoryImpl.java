package com.spvermicelli.tripledger.billing.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spvermicelli.tripledger.billing.domain.request.model.BillChangeRequest;
import com.spvermicelli.tripledger.billing.domain.request.repository.BillChangeRequestRepository;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.mapper.BillChangeRequestMapper;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.po.BillChangeRequestPO;
import com.spvermicelli.tripledger.shared.domain.enums.ChangeRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class BillChangeRequestRepositoryImpl implements BillChangeRequestRepository {

    private final BillChangeRequestMapper billChangeRequestMapper;

    public BillChangeRequestRepositoryImpl(BillChangeRequestMapper billChangeRequestMapper) {
        this.billChangeRequestMapper = billChangeRequestMapper;
    }

    @Override
    public BillChangeRequest save(BillChangeRequest request) {
        BillChangeRequestPO po = toPO(request);
        if (po.getId() == null) {
            billChangeRequestMapper.insert(po);
        } else {
            billChangeRequestMapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<BillChangeRequest> findById(Long requestId) {
        return Optional.ofNullable(billChangeRequestMapper.selectById(requestId)).map(this::toDomain);
    }

    @Override
    public List<BillChangeRequest> findByBillId(Long billId) {
        return billChangeRequestMapper.selectList(new LambdaQueryWrapper<BillChangeRequestPO>()
                .eq(BillChangeRequestPO::getBillId, billId)
                .orderByDesc(BillChangeRequestPO::getCreatedAt)
                .orderByDesc(BillChangeRequestPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<BillChangeRequest> findByBookIdAndRequesterMemberId(Long bookId, Long requesterMemberId) {
        return billChangeRequestMapper.selectList(new LambdaQueryWrapper<BillChangeRequestPO>()
                .inSql(BillChangeRequestPO::getBillId, "select id from tb_bill where book_id = " + bookId)
                .eq(BillChangeRequestPO::getRequesterMemberId, requesterMemberId)
                .orderByDesc(BillChangeRequestPO::getCreatedAt)
                .orderByDesc(BillChangeRequestPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<BillChangeRequest> findByBookIdAndStatus(Long bookId, ChangeRequestStatus status) {
        return billChangeRequestMapper.selectList(new LambdaQueryWrapper<BillChangeRequestPO>()
                .inSql(BillChangeRequestPO::getBillId, "select id from tb_bill where book_id = " + bookId)
                .eq(BillChangeRequestPO::getStatus, status)
                .orderByDesc(BillChangeRequestPO::getCreatedAt)
                .orderByDesc(BillChangeRequestPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    private BillChangeRequest toDomain(BillChangeRequestPO po) {
        return BillChangeRequest.builder()
            .id(po.getId())
            .billId(po.getBillId())
            .predecessorRequestId(po.getPredecessorRequestId())
            .baseline(Boolean.TRUE.equals(po.getBaseline()))
            .requestType(po.getRequestType())
            .requesterMemberId(po.getRequesterMemberId())
            .requestReason(po.getRequestReason())
            .status(po.getStatus())
            .createdAt(po.getCreatedAt())
            .handledAt(po.getHandledAt())
            .build();
    }

    private BillChangeRequestPO toPO(BillChangeRequest request) {
        BillChangeRequestPO po = new BillChangeRequestPO();
        po.setId(request.getId());
        po.setBillId(request.getBillId());
        po.setPredecessorRequestId(request.getPredecessorRequestId());
        po.setBaseline(request.isBaseline());
        po.setRequestType(request.getRequestType());
        po.setRequesterMemberId(request.getRequesterMemberId());
        po.setRequestReason(request.getRequestReason());
        po.setStatus(request.getStatus());
        po.setCreatedAt(request.getCreatedAt());
        po.setHandledAt(request.getHandledAt());
        return po;
    }
}
