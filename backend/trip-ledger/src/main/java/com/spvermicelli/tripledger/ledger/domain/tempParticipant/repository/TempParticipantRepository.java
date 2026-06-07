package com.spvermicelli.tripledger.ledger.domain.tempParticipant.repository;

import com.spvermicelli.tripledger.ledger.domain.tempParticipant.model.TempParticipant;
import java.util.List;
import java.util.Optional;

/**
 * 临时成员仓储。
 * 统一抽象临时成员的读写能力，避免应用层直接依赖 MyBatis 条件拼装。
 */
public interface TempParticipantRepository {

    TempParticipant save(TempParticipant tempParticipant);

    Optional<TempParticipant> findById(Long tempParticipantId);

    List<TempParticipant> findByBookId(Long bookId);

    List<TempParticipant> findActiveByBookId(Long bookId);

    Optional<TempParticipant> findByBookIdAndNickname(Long bookId, String nickname);
}
