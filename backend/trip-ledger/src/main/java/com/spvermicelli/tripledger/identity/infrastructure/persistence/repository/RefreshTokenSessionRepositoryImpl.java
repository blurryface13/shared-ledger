package com.spvermicelli.tripledger.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spvermicelli.tripledger.identity.domain.auth.model.RefreshTokenSession;
import com.spvermicelli.tripledger.identity.domain.auth.repository.RefreshTokenSessionRepository;
import com.spvermicelli.tripledger.identity.infrastructure.persistence.mapper.RefreshTokenSessionMapper;
import com.spvermicelli.tripledger.identity.infrastructure.persistence.po.RefreshTokenSessionPO;
import com.spvermicelli.tripledger.identity.domain.auth.valueobject.RefreshTokenStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * refresh token 仓储实现。
 */
@Repository
public class RefreshTokenSessionRepositoryImpl implements RefreshTokenSessionRepository {

    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();

    private final RefreshTokenSessionMapper refreshTokenSessionMapper;

    public RefreshTokenSessionRepositoryImpl(RefreshTokenSessionMapper refreshTokenSessionMapper) {
        this.refreshTokenSessionMapper = refreshTokenSessionMapper;
    }

    @Override
    public Optional<RefreshTokenSession> findByTokenHash(String tokenHash) {
        LambdaQueryWrapper<RefreshTokenSessionPO> queryWrapper = new LambdaQueryWrapper<RefreshTokenSessionPO>()
            .eq(RefreshTokenSessionPO::getTokenHash, tokenHash);
        return Optional.ofNullable(refreshTokenSessionMapper.selectOne(queryWrapper)).map(this::toDomain);
    }

    @Override
    public List<RefreshTokenSession> findActiveByUserId(Long userId) {
        return refreshTokenSessionMapper.selectList(new LambdaQueryWrapper<RefreshTokenSessionPO>()
                .eq(RefreshTokenSessionPO::getUserId, userId)
                .eq(RefreshTokenSessionPO::getStatus, RefreshTokenStatus.ACTIVE))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public RefreshTokenSession save(RefreshTokenSession session) {
        RefreshTokenSessionPO po = toPO(session);
        if (po.getId() == null) {
            refreshTokenSessionMapper.insert(po);
        } else {
            refreshTokenSessionMapper.updateById(po);
        }
        return toDomain(po);
    }

    private RefreshTokenSession toDomain(RefreshTokenSessionPO po) {
        return RefreshTokenSession.builder()
            .id(po.getId())
            .userId(po.getUserId())
            .tokenHash(po.getTokenHash())
            .status(po.getStatus())
            .expireAt(toInstant(po.getExpireAt()))
            .lastUsedAt(toInstant(po.getLastUsedAt()))
            .revokedAt(toInstant(po.getRevokedAt()))
            .replacedByTokenHash(po.getReplacedByTokenHash())
            .createdAt(po.getCreatedAt())
            .updatedAt(po.getUpdatedAt())
            .build();
    }

    private RefreshTokenSessionPO toPO(RefreshTokenSession session) {
        RefreshTokenSessionPO po = new RefreshTokenSessionPO();
        po.setId(session.getId());
        po.setUserId(session.getUserId());
        po.setTokenHash(session.getTokenHash());
        po.setStatus(session.getStatus());
        po.setExpireAt(toLocalDateTime(session.getExpireAt()));
        po.setLastUsedAt(toLocalDateTime(session.getLastUsedAt()));
        po.setRevokedAt(toLocalDateTime(session.getRevokedAt()));
        po.setReplacedByTokenHash(session.getReplacedByTokenHash());
        po.setCreatedAt(session.getCreatedAt());
        po.setUpdatedAt(session.getUpdatedAt());
        return po;
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(DEFAULT_ZONE_ID).toInstant();
    }

    private LocalDateTime toLocalDateTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, DEFAULT_ZONE_ID);
    }
}
