package com.spvermicelli.tripledger.ledger.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spvermicelli.tripledger.ledger.domain.invitation.model.BookInvitation;
import com.spvermicelli.tripledger.ledger.domain.invitation.repository.BookInvitationRepository;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.mapper.BookInvitationMapper;
import com.spvermicelli.tripledger.ledger.infrastructure.persistence.po.BookInvitationPO;
import com.spvermicelli.tripledger.shared.domain.enums.InvitationStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class BookInvitationRepositoryImpl implements BookInvitationRepository {

    private final BookInvitationMapper bookInvitationMapper;

    public BookInvitationRepositoryImpl(BookInvitationMapper bookInvitationMapper) {
        this.bookInvitationMapper = bookInvitationMapper;
    }

    @Override
    public BookInvitation save(BookInvitation invitation) {
        BookInvitationPO po = toPO(invitation);
        if (po.getId() == null) {
            bookInvitationMapper.insert(po);
        } else {
            bookInvitationMapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<BookInvitation> findById(Long invitationId) {
        return Optional.ofNullable(bookInvitationMapper.selectById(invitationId)).map(this::toDomain);
    }

    @Override
    public Optional<BookInvitation> findPendingByBookIdAndInviteeUserId(Long bookId, Long inviteeUserId) {
        return Optional.ofNullable(bookInvitationMapper.selectOne(new LambdaQueryWrapper<BookInvitationPO>()
                .eq(BookInvitationPO::getBookId, bookId)
                .eq(BookInvitationPO::getInviteeUserId, inviteeUserId)
                .eq(BookInvitationPO::getStatus, InvitationStatus.PENDING)
                .orderByDesc(BookInvitationPO::getId)
                .last("LIMIT 1")))
            .map(this::toDomain);
    }

    @Override
    public List<BookInvitation> findPendingByBookIdAndInviteeUserIds(Long bookId, Collection<Long> inviteeUserIds) {
        if (inviteeUserIds == null || inviteeUserIds.isEmpty()) {
            return List.of();
        }
        return bookInvitationMapper.selectList(new LambdaQueryWrapper<BookInvitationPO>()
                .eq(BookInvitationPO::getBookId, bookId)
                .in(BookInvitationPO::getInviteeUserId, inviteeUserIds)
                .eq(BookInvitationPO::getStatus, InvitationStatus.PENDING)
                .orderByDesc(BookInvitationPO::getInvitedAt)
                .orderByDesc(BookInvitationPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<BookInvitation> findPendingByInviteeUserId(Long inviteeUserId) {
        return bookInvitationMapper.selectList(new LambdaQueryWrapper<BookInvitationPO>()
                .eq(BookInvitationPO::getInviteeUserId, inviteeUserId)
                .eq(BookInvitationPO::getStatus, InvitationStatus.PENDING)
                .orderByDesc(BookInvitationPO::getInvitedAt)
                .orderByDesc(BookInvitationPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<BookInvitation> findByInviteeUserId(Long inviteeUserId) {
        return bookInvitationMapper.selectList(new LambdaQueryWrapper<BookInvitationPO>()
                .eq(BookInvitationPO::getInviteeUserId, inviteeUserId)
                .orderByDesc(BookInvitationPO::getInvitedAt)
                .orderByDesc(BookInvitationPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<BookInvitation> findByInviterUserId(Long inviterUserId) {
        return bookInvitationMapper.selectList(new LambdaQueryWrapper<BookInvitationPO>()
                .eq(BookInvitationPO::getInviterUserId, inviterUserId)
                .orderByDesc(BookInvitationPO::getInvitedAt)
                .orderByDesc(BookInvitationPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    private BookInvitation toDomain(BookInvitationPO po) {
        return BookInvitation.builder()
            .id(po.getId())
            .bookId(po.getBookId())
            .inviterUserId(po.getInviterUserId())
            .inviteeUserId(po.getInviteeUserId())
            .status(po.getStatus())
            .invitedAt(po.getInvitedAt())
            .handledAt(po.getHandledAt())
            .remark(po.getRemark())
            .build();
    }

    private BookInvitationPO toPO(BookInvitation invitation) {
        BookInvitationPO po = new BookInvitationPO();
        po.setId(invitation.getId());
        po.setBookId(invitation.getBookId());
        po.setInviterUserId(invitation.getInviterUserId());
        po.setInviteeUserId(invitation.getInviteeUserId());
        po.setStatus(invitation.getStatus());
        po.setInvitedAt(invitation.getInvitedAt());
        po.setHandledAt(invitation.getHandledAt());
        po.setRemark(invitation.getRemark());
        return po;
    }
}
