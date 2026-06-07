package com.spvermicelli.tripledger.ledger.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spvermicelli.tripledger.ledger.domain.category.model.BookCategory;
import com.spvermicelli.tripledger.ledger.domain.category.repository.BookCategoryRepository;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.mapper.BookCategoryMapper;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.po.BookCategoryPO;
import com.spvermicelli.tripledger.shared.domain.enums.CategorySource;
import com.spvermicelli.tripledger.shared.domain.enums.CategoryStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 分类仓储实现。
 * 这里统一承接分类的持久化映射与“可见分类集合”的查询规则，
 * 避免控制器或应用服务直接拼装 SQL 条件。
 */
@Repository
public class BookCategoryRepositoryImpl implements BookCategoryRepository {

    private final BookCategoryMapper bookCategoryMapper;

    public BookCategoryRepositoryImpl(BookCategoryMapper bookCategoryMapper) {
        this.bookCategoryMapper = bookCategoryMapper;
    }

    @Override
    public BookCategory save(BookCategory category) {
        BookCategoryPO po = toPO(category);
        if (po.getId() == null) {
            bookCategoryMapper.insert(po);
        } else {
            bookCategoryMapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<BookCategory> findById(Long categoryId) {
        return Optional.ofNullable(bookCategoryMapper.selectById(categoryId)).map(this::toDomain);
    }

    @Override
    public List<BookCategory> findVisibleForBook(Long bookId, Long currentUserId) {
        return bookCategoryMapper.selectList(new LambdaQueryWrapper<BookCategoryPO>()
                .eq(BookCategoryPO::getStatus, CategoryStatus.ACTIVE)
                .and(wrapper -> wrapper
                    .eq(BookCategoryPO::getCategorySource, CategorySource.SYSTEM)
                    .or(inner -> inner
                        .eq(BookCategoryPO::getCategorySource, CategorySource.CUSTOM_GLOBAL)
                        .eq(BookCategoryPO::getBookId, bookId))
                    .or(inner -> inner
                        .eq(BookCategoryPO::getCategorySource, CategorySource.CUSTOM_LOCAL)
                        .eq(BookCategoryPO::getBookId, bookId)
                        .eq(BookCategoryPO::getCreatedByUserId, currentUserId)))
                .orderByAsc(BookCategoryPO::getSortOrder)
                .orderByAsc(BookCategoryPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public int findNextSortOrder(Long currentUserId, CategorySource categorySource, Long bookId) {
        LambdaQueryWrapper<BookCategoryPO> query = new LambdaQueryWrapper<BookCategoryPO>()
            .eq(BookCategoryPO::getCategorySource, categorySource)
            .eq(BookCategoryPO::getBookId, bookId)
            .orderByDesc(BookCategoryPO::getSortOrder)
            .last("LIMIT 1");
        if (categorySource == CategorySource.CUSTOM_LOCAL) {
            query.eq(BookCategoryPO::getCreatedByUserId, currentUserId);
        }
        Integer maxSortOrder = bookCategoryMapper.selectList(query)
            .stream()
            .findFirst()
            .map(BookCategoryPO::getSortOrder)
            .orElse(0);
        return maxSortOrder + 1;
    }

    @Override
    public boolean existsSameNameInScope(
        Long currentUserId,
        CategorySource categorySource,
        Long bookId,
        String name,
        Long excludeId
    ) {
        LambdaQueryWrapper<BookCategoryPO> query = new LambdaQueryWrapper<BookCategoryPO>()
            .eq(BookCategoryPO::getCategorySource, categorySource)
            .eq(BookCategoryPO::getBookId, bookId)
            .eq(BookCategoryPO::getName, name)
            .ne(excludeId != null, BookCategoryPO::getId, excludeId);
        if (categorySource == CategorySource.CUSTOM_LOCAL) {
            query.eq(BookCategoryPO::getCreatedByUserId, currentUserId);
        }
        return bookCategoryMapper.selectCount(query) > 0;
    }

    private BookCategory toDomain(BookCategoryPO po) {
        return BookCategory.builder()
            .id(po.getId())
            .bookId(po.getBookId())
            .name(po.getName())
            .icon(po.getIcon())
            .categoryType(po.getCategoryType())
            .categorySource(po.getCategorySource())
            .status(po.getStatus())
            .sortOrder(po.getSortOrder())
            .createdByUserId(po.getCreatedByUserId())
            .createdAt(po.getCreatedAt())
            .updatedAt(po.getUpdatedAt())
            .build();
    }

    private BookCategoryPO toPO(BookCategory category) {
        BookCategoryPO po = new BookCategoryPO();
        po.setId(category.getId());
        po.setBookId(category.getBookId());
        po.setName(category.getName());
        po.setIcon(category.getIcon());
        po.setCategoryType(category.getCategoryType());
        po.setCategorySource(category.getCategorySource());
        po.setStatus(category.getStatus());
        po.setSortOrder(category.getSortOrder());
        po.setCreatedByUserId(category.getCreatedByUserId());
        po.setCreatedAt(category.getCreatedAt());
        po.setUpdatedAt(category.getUpdatedAt());
        return po;
    }
}
