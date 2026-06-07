package com.spvermicelli.tripledger.identity.domain.auth.repository;

import com.spvermicelli.tripledger.identity.domain.auth.model.RefreshTokenSession;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenSessionRepository {

    Optional<RefreshTokenSession> findByTokenHash(String tokenHash);

    List<RefreshTokenSession> findActiveByUserId(Long userId);

    RefreshTokenSession save(RefreshTokenSession session);
}
