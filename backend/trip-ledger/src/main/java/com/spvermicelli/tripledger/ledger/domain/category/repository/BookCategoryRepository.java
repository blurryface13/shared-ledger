package com.spvermicelli.tripledger.ledger.domain.category.repository;

import com.spvermicelli.tripledger.ledger.domain.category.model.BookCategory;
import com.spvermicelli.tripledger.shared.domain.enums.CategorySource;
import java.util.List;
import java.util.Optional;

public interface BookCategoryRepository {

    BookCategory save(BookCategory category);

    Optional<BookCategory> findById(Long categoryId);

    List<BookCategory> findVisibleForBook(Long bookId, Long currentUserId);

    int findNextSortOrder(Long currentUserId, CategorySource categorySource, Long bookId);

    boolean existsSameNameInScope(Long currentUserId, CategorySource categorySource, Long bookId, String name, Long excludeId);
}
