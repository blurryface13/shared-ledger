package com.spvermicelli.tripledger.ledger.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spvermicelli.tripledger.ledger.domain.book.model.BookMember;
import com.spvermicelli.tripledger.ledger.domain.book.repository.BookMemberRepository;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.mapper.BookMemberMapper;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.po.BookMemberPO;
import com.spvermicelli.tripledger.shared.domain.enums.MemberStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class BookMemberRepositoryImpl implements BookMemberRepository {

    private final BookMemberMapper bookMemberMapper;

    public BookMemberRepositoryImpl(BookMemberMapper bookMemberMapper) {
        this.bookMemberMapper = bookMemberMapper;
    }

    @Override
    public BookMember save(BookMember bookMember) {
        BookMemberPO po = toPO(bookMember);
        if (po.getId() == null) {
            bookMemberMapper.insert(po);
        } else {
            bookMemberMapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<BookMember> findById(Long id) {
        return Optional.ofNullable(bookMemberMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<BookMember> findByBookIdAndUserId(Long bookId, Long userId) {
        return Optional.ofNullable(bookMemberMapper.selectOne(new LambdaQueryWrapper<BookMemberPO>()
                .eq(BookMemberPO::getBookId, bookId)
                .eq(BookMemberPO::getUserId, userId)))
            .map(this::toDomain);
    }

    @Override
    public List<BookMember> findByBookId(Long bookId) {
        return bookMemberMapper.selectList(new LambdaQueryWrapper<BookMemberPO>()
                .eq(BookMemberPO::getBookId, bookId)
                .orderByAsc(BookMemberPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<BookMember> findActiveByBookId(Long bookId) {
        return bookMemberMapper.selectList(new LambdaQueryWrapper<BookMemberPO>()
                .eq(BookMemberPO::getBookId, bookId)
                .eq(BookMemberPO::getMemberStatus, MemberStatus.ACTIVE)
                .orderByAsc(BookMemberPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<BookMember> findActiveByUserId(Long userId) {
        return bookMemberMapper.selectList(new LambdaQueryWrapper<BookMemberPO>()
                .eq(BookMemberPO::getUserId, userId)
                .eq(BookMemberPO::getMemberStatus, MemberStatus.ACTIVE)
                .orderByDesc(BookMemberPO::getUpdatedAt)
                .orderByDesc(BookMemberPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    private BookMember toDomain(BookMemberPO po) {
        return BookMember.builder()
            .id(po.getId())
            .bookId(po.getBookId())
            .userId(po.getUserId())
            .budgetAmountCent(po.getBudgetAmountCent())
            .memberRole(po.getMemberRole())
            .memberStatus(po.getMemberStatus())
            .joinedAt(po.getJoinedAt())
            .leftAt(po.getLeftAt())
            .invitedByUserId(po.getInvitedByUserId())
            .createdAt(po.getCreatedAt())
            .updatedAt(po.getUpdatedAt())
            .build();
    }

    private BookMemberPO toPO(BookMember domain) {
        BookMemberPO po = new BookMemberPO();
        po.setId(domain.getId());
        po.setBookId(domain.getBookId());
        po.setUserId(domain.getUserId());
        po.setBudgetAmountCent(domain.getBudgetAmountCent());
        po.setMemberRole(domain.getMemberRole());
        po.setMemberStatus(domain.getMemberStatus());
        po.setJoinedAt(domain.getJoinedAt());
        po.setLeftAt(domain.getLeftAt());
        po.setInvitedByUserId(domain.getInvitedByUserId());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }
}
