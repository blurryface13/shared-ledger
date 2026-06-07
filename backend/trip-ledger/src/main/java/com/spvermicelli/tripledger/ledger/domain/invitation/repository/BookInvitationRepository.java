package com.spvermicelli.tripledger.ledger.domain.invitation.repository;

import com.spvermicelli.tripledger.ledger.domain.invitation.model.BookInvitation;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookInvitationRepository {

    BookInvitation save(BookInvitation invitation);

    Optional<BookInvitation> findById(Long invitationId);

    Optional<BookInvitation> findPendingByBookIdAndInviteeUserId(Long bookId, Long inviteeUserId);

    List<BookInvitation> findPendingByBookIdAndInviteeUserIds(Long bookId, Collection<Long> inviteeUserIds);

    List<BookInvitation> findPendingByInviteeUserId(Long inviteeUserId);

    List<BookInvitation> findByInviteeUserId(Long inviteeUserId);

    List<BookInvitation> findByInviterUserId(Long inviterUserId);
}
