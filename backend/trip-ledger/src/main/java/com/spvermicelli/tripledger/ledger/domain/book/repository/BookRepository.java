package com.spvermicelli.tripledger.ledger.domain.book.repository;

import com.spvermicelli.tripledger.ledger.domain.book.model.Book;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookRepository {

    Book save(Book book);

    Optional<Book> findById(Long id);

    List<Book> findByIds(Collection<Long> ids);

    /**
     * 判断同一创建者名下是否已存在同名账本。
     *
     * @param ownerUserId 创建者 userId
     * @param name 账本名称（建议调用方传入 trim 后值）
     * @param excludeBookId 排除的账本 ID（更新时使用，创建时传 null）
     */
    boolean existsByOwnerUserIdAndName(Long ownerUserId, String name, Long excludeBookId);
}
