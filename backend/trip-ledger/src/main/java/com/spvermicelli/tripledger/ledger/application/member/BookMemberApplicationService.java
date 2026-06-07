package com.spvermicelli.tripledger.ledger.application.member;

import com.spvermicelli.tripledger.ledger.application.member.command.CancelBookAdminCommand;
import com.spvermicelli.tripledger.ledger.application.member.command.QuitBookCommand;
import com.spvermicelli.tripledger.ledger.application.member.command.RemoveBookMemberCommand;
import com.spvermicelli.tripledger.ledger.application.member.command.SetBookAdminCommand;
import com.spvermicelli.tripledger.ledger.application.member.command.TransferBookOwnerCommand;
import com.spvermicelli.tripledger.ledger.application.member.result.BookMemberListItemResult;
import com.spvermicelli.tripledger.ledger.application.member.result.MemberOperationResult;
import com.spvermicelli.tripledger.ledger.domain.book.model.Book;
import com.spvermicelli.tripledger.ledger.domain.book.model.BookMember;
import com.spvermicelli.tripledger.ledger.domain.book.repository.BookMemberRepository;
import com.spvermicelli.tripledger.ledger.domain.book.repository.BookRepository;
import com.spvermicelli.tripledger.ledger.domain.book.service.BookUserDirectory;
import com.spvermicelli.tripledger.ledger.domain.book.valueobject.BookUserSummary;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.spvermicelli.tripledger.shared.domain.enums.MemberRole;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 账本成员应用服务。
 * 该服务统一处理成员查看、退出、移除、角色切换和创建者转让等账本级高风险操作。
 * 所有权限都根据 token 中的当前用户身份在后端重新计算，不信任前端任何“我是谁、我是什么角色”的传参。
 */
@Service
public class BookMemberApplicationService {

    private final BookRepository bookRepository;
    private final BookMemberRepository bookMemberRepository;
    private final BookUserDirectory bookUserDirectory;

    public BookMemberApplicationService(
        BookRepository bookRepository,
        BookMemberRepository bookMemberRepository,
        BookUserDirectory bookUserDirectory
    ) {
        this.bookRepository = bookRepository;
        this.bookMemberRepository = bookMemberRepository;
        this.bookUserDirectory = bookUserDirectory;
    }

    @Transactional(readOnly = true)
    public List<BookMemberListItemResult> getBookMembers(Long currentUserId, Long bookId) {
        validateCurrentUser(currentUserId);
        ensureActiveMembership(bookId, currentUserId);

        List<BookMember> members = bookMemberRepository.findByBookId(bookId);
        Map<Long, BookUserSummary> userSummaryMap = bookUserDirectory.getByUserIds(
            members.stream().map(BookMember::getUserId).collect(Collectors.toSet())
        );

        return members.stream()
            .sorted(this::compareMemberForList)
            .map(member -> {
                BookUserSummary summary = userSummaryMap.get(member.getUserId());
                return BookMemberListItemResult.builder()
                    .memberId(member.getId())
                    .userId(member.getUserId())
                    .nickname(summary == null ? null : summary.getNickname())
                    .avatarUrl(summary == null ? null : summary.getAvatarUrl())
                    .phoneNumber(summary == null ? null : summary.getPhoneNumber())
                    .memberRole(member.getMemberRole().getCode())
                    .memberStatus(member.getMemberStatus().getCode())
                    .joinedAt(member.getJoinedAt())
                    .leftAt(member.getLeftAt())
                    .build();
            })
            .toList();
    }

    @Transactional
    public MemberOperationResult quitBook(QuitBookCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        Book book = loadBook(command.getBookId());
        ensureSharedBook(book);

        BookMember currentMember = ensureActiveMembership(book.getId(), command.getCurrentUserId());
        if (currentMember.isOwner()) {
            throw new BusinessException(ErrorCode.CONFLICT, "创建者不能直接退出账本，请先转让创建者身份");
        }

        bookMemberRepository.save(currentMember.quit(LocalDateTime.now()));
        return MemberOperationResult.builder()
            .message("你已成功退出账本")
            .build();
    }

    @Transactional
    public MemberOperationResult removeMember(RemoveBookMemberCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        Book book = loadBook(command.getBookId());
        ensureSharedBook(book);

        BookMember operator = ensureActiveMembership(book.getId(), command.getCurrentUserId());
        if (!operator.canManageMembers()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有创建者或管理员可以移除成员");
        }

        BookMember targetMember = loadActiveMember(command.getMemberId(), command.getBookId());
        if (targetMember.getUserId().equals(command.getCurrentUserId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "不能通过移除接口操作自己，如需离开请使用退出账本接口");
        }
        if (targetMember.isOwner()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "创建者不能被移除");
        }

        bookMemberRepository.save(targetMember.remove(LocalDateTime.now()));
        return MemberOperationResult.builder()
            .message("成员已移出账本")
            .build();
    }

    @Transactional
    public MemberOperationResult setAdmin(SetBookAdminCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        Book book = loadOwnedSharedBook(command.getBookId(), command.getCurrentUserId());

        BookMember targetMember = loadActiveMember(command.getMemberId(), book.getId());
        if (targetMember.isOwner()) {
            throw new BusinessException(ErrorCode.CONFLICT, "创建者不需要重复设置为管理员");
        }
        if (targetMember.isAdmin()) {
            throw new BusinessException(ErrorCode.CONFLICT, "该成员已经是管理员");
        }

        bookMemberRepository.save(targetMember.changeRole(MemberRole.ADMIN));
        return MemberOperationResult.builder()
            .message("已设置为管理员")
            .build();
    }

    @Transactional
    public MemberOperationResult cancelAdmin(CancelBookAdminCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        Book book = loadOwnedSharedBook(command.getBookId(), command.getCurrentUserId());

        BookMember targetMember = loadActiveMember(command.getMemberId(), book.getId());
        if (targetMember.isOwner()) {
            throw new BusinessException(ErrorCode.CONFLICT, "创建者角色不能取消");
        }
        if (!targetMember.isAdmin()) {
            throw new BusinessException(ErrorCode.CONFLICT, "该成员当前不是管理员");
        }

        bookMemberRepository.save(targetMember.changeRole(MemberRole.MEMBER));
        return MemberOperationResult.builder()
            .message("已取消管理员身份")
            .build();
    }

    @Transactional
    public MemberOperationResult transferOwner(TransferBookOwnerCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        Book book = loadOwnedSharedBook(command.getBookId(), command.getCurrentUserId());

        BookMember currentOwner = ensureActiveMembership(book.getId(), command.getCurrentUserId());
        BookMember targetMember = loadActiveMember(command.getTargetMemberId(), book.getId());

        if (targetMember.getUserId().equals(command.getCurrentUserId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "目标成员已经是当前创建者，无需重复转让");
        }

        bookRepository.save(book.transferOwnership(targetMember.getUserId()));
        bookMemberRepository.save(currentOwner.changeRole(MemberRole.MEMBER));
        bookMemberRepository.save(targetMember.changeRole(MemberRole.OWNER));

        return MemberOperationResult.builder()
            .message("创建者身份已成功转让")
            .build();
    }

    private void validateCurrentUser(Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前登录信息不存在");
        }
    }

    private Book loadOwnedSharedBook(Long bookId, Long currentUserId) {
        Book book = loadBook(bookId);
        ensureSharedBook(book);
        if (!currentUserId.equals(book.getOwnerUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅账本创建者可以执行该操作");
        }
        ensureActiveMembership(bookId, currentUserId);
        return book;
    }

    private Book loadBook(Long bookId) {
        if (bookId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "bookId 不能为空");
        }
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "账本不存在"));
        if (!book.isActive()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账本不存在");
        }
        return book;
    }

    private void ensureSharedBook(Book book) {
        if (!book.isShared()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "个人账本不支持成员管理操作");
        }
    }

    private BookMember ensureActiveMembership(Long bookId, Long currentUserId) {
        return bookMemberRepository.findByBookIdAndUserId(bookId, currentUserId)
            .filter(BookMember::isActive)
            .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "当前用户不在该账本中"));
    }

    private BookMember loadActiveMember(Long memberId, Long bookId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "memberId 不能为空");
        }
        BookMember member = bookMemberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "目标成员不存在"));
        if (!member.getBookId().equals(bookId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "目标成员不属于当前账本");
        }
        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "目标成员当前不是有效成员");
        }
        return member;
    }

    private int compareMemberForList(BookMember left, BookMember right) {
        int statusCompare = Integer.compare(memberStatusWeight(left), memberStatusWeight(right));
        if (statusCompare != 0) {
            return statusCompare;
        }

        int roleCompare = Integer.compare(memberRoleWeight(left), memberRoleWeight(right));
        if (roleCompare != 0) {
            return roleCompare;
        }

        Comparator<LocalDateTime> dateTimeComparator = Comparator.nullsLast(LocalDateTime::compareTo);
        LocalDateTime leftTime = left.isActive() ? left.getJoinedAt() : left.getLeftAt();
        LocalDateTime rightTime = right.isActive() ? right.getJoinedAt() : right.getLeftAt();
        return dateTimeComparator.reversed().compare(leftTime, rightTime);
    }

    private int memberStatusWeight(BookMember member) {
        if (member.isActive()) {
            return 0;
        }
        return "QUIT".equals(member.getMemberStatus().getCode()) ? 1 : 2;
    }

    private int memberRoleWeight(BookMember member) {
        if (member.isOwner()) {
            return 0;
        }
        return member.isAdmin() ? 1 : 2;
    }
}
