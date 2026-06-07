package com.spvermicelli.tripledger.ledger.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spvermicelli.tripledger.ledger.domain.tempParticipant.model.TempParticipant;
import com.spvermicelli.tripledger.ledger.domain.tempParticipant.repository.TempParticipantRepository;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.mapper.TempParticipantMapper;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.po.TempParticipantPO;
import com.spvermicelli.tripledger.shared.domain.enums.TempParticipantStatus;
import com.spvermicelli.tripledger.shared.domain.enums.TempParticipantType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 临时成员仓储实现。
 * 该实现负责持久化临时成员，并提供账本维度的查询能力。
 */
@Repository
public class TempParticipantRepositoryImpl implements TempParticipantRepository {

    private final TempParticipantMapper tempParticipantMapper;

    public TempParticipantRepositoryImpl(TempParticipantMapper tempParticipantMapper) {
        this.tempParticipantMapper = tempParticipantMapper;
    }

    @Override
    public TempParticipant save(TempParticipant tempParticipant) {
        TempParticipantPO po = toPO(tempParticipant);
        if (po.getId() == null) {
            tempParticipantMapper.insert(po);
        } else {
            tempParticipantMapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<TempParticipant> findById(Long tempParticipantId) {
        return Optional.ofNullable(tempParticipantMapper.selectById(tempParticipantId)).map(this::toDomain);
    }

    @Override
    public List<TempParticipant> findByBookId(Long bookId) {
        return tempParticipantMapper.selectList(new LambdaQueryWrapper<TempParticipantPO>()
                .eq(TempParticipantPO::getBookId, bookId)
                .orderByAsc(TempParticipantPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<TempParticipant> findActiveByBookId(Long bookId) {
        return tempParticipantMapper.selectList(new LambdaQueryWrapper<TempParticipantPO>()
                .eq(TempParticipantPO::getBookId, bookId)
                .eq(TempParticipantPO::getStatus, TempParticipantStatus.ACTIVE)
                .orderByAsc(TempParticipantPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public Optional<TempParticipant> findByBookIdAndNickname(Long bookId, String nickname) {
        return Optional.ofNullable(tempParticipantMapper.selectOne(new LambdaQueryWrapper<TempParticipantPO>()
                .eq(TempParticipantPO::getBookId, bookId)
                .eq(TempParticipantPO::getNickname, nickname)
                .last("LIMIT 1")))
            .map(this::toDomain);
    }

    private TempParticipant toDomain(TempParticipantPO po) {
        return TempParticipant.builder()
            .id(po.getId())
            .bookId(po.getBookId())
            .nickname(po.getNickname())
            .tempType(TempParticipantType.normalize(po.getTempType()))
            .createdByMemberId(po.getCreatedByMemberId())
            .attachedMemberId(po.getAttachedMemberId())
            .status(po.getStatus())
            .createdAt(po.getCreatedAt())
            .updatedAt(po.getUpdatedAt())
            .build();
    }

    private TempParticipantPO toPO(TempParticipant domain) {
        TempParticipantPO po = new TempParticipantPO();
        po.setId(domain.getId());
        po.setBookId(domain.getBookId());
        po.setNickname(domain.getNickname());
        po.setTempType(TempParticipantType.normalize(domain.getTempType()));
        po.setCreatedByMemberId(domain.getCreatedByMemberId());
        po.setAttachedMemberId(domain.getAttachedMemberId());
        po.setStatus(domain.getStatus());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }
}
