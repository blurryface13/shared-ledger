package com.spvermicelli.tripledger.ledger.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spvermicelli.tripledger.ledger.domain.book.model.Book;
import com.spvermicelli.tripledger.ledger.domain.book.repository.BookRepository;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.mapper.BookMapper;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.po.BookPO;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class BookRepositoryImpl implements BookRepository {

    private final BookMapper bookMapper;

    public BookRepositoryImpl(BookMapper bookMapper) {
        this.bookMapper = bookMapper;
    }

    @Override
    public Book save(Book book) {
        BookPO bookPO = toPO(book);
        if (bookPO.getId() == null) {
            bookMapper.insert(bookPO);
        } else {
            bookMapper.updateById(bookPO);
        }
        return toDomain(bookPO);
    }

    @Override
    public Optional<Book> findById(Long id) {
        return Optional.ofNullable(bookMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<Book> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return bookMapper.selectList(new LambdaQueryWrapper<BookPO>().in(BookPO::getId, ids))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public boolean existsByOwnerUserIdAndName(Long ownerUserId, String name, Long excludeBookId) {
        LambdaQueryWrapper<BookPO> wrapper = Wrappers.lambdaQuery(BookPO.class)
            .eq(BookPO::getOwnerUserId, ownerUserId)
            .eq(BookPO::getName, name);
        if (excludeBookId != null) {
            wrapper.ne(BookPO::getId, excludeBookId);
        }
        return bookMapper.selectCount(wrapper) > 0;
    }

    private Book toDomain(BookPO bookPO) {
        return Book.builder()
            .id(bookPO.getId())
            .name(bookPO.getName())
            .bookType(bookPO.getBookType())
            .ownerUserId(bookPO.getOwnerUserId())
            .description(bookPO.getDescription())
            .coverUrl(bookPO.getCoverUrl())
            .status(bookPO.getStatus())
            .createdAt(bookPO.getCreatedAt())
            .updatedAt(bookPO.getUpdatedAt())
            .build();
    }

    private BookPO toPO(Book book) {
        BookPO bookPO = new BookPO();
        bookPO.setId(book.getId());
        bookPO.setName(book.getName());
        bookPO.setBookType(book.getBookType());
        bookPO.setOwnerUserId(book.getOwnerUserId());
        bookPO.setDescription(book.getDescription());
        bookPO.setCoverUrl(book.getCoverUrl());
        bookPO.setStatus(book.getStatus());
        bookPO.setCreatedAt(book.getCreatedAt());
        bookPO.setUpdatedAt(book.getUpdatedAt());
        return bookPO;
    }
}
