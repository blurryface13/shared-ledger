package com.spvermicelli.tripledger.ledger.domain.book.repository;

import com.spvermicelli.tripledger.ledger.domain.book.model.BookMember;
import java.util.List;
import java.util.Optional;

public interface BookMemberRepository {

    BookMember save(BookMember bookMember);

    Optional<BookMember> findById(Long id);

    Optional<BookMember> findByBookIdAndUserId(Long bookId, Long userId);

    List<BookMember> findByBookId(Long bookId);

    List<BookMember> findActiveByBookId(Long bookId);

    List<BookMember> findActiveByUserId(Long userId);
}
