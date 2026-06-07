package com.spvermicelli.tripledger.ledger.application.book;

import com.spvermicelli.tripledger.ledger.application.book.command.CreateBookCommand;
import com.spvermicelli.tripledger.ledger.application.book.command.DeleteBookCommand;
import com.spvermicelli.tripledger.ledger.application.book.command.UpdateBookCommand;
import com.spvermicelli.tripledger.ledger.application.book.result.BookDetailResult;
import com.spvermicelli.tripledger.ledger.application.book.result.BookListItemResult;
import com.spvermicelli.tripledger.ledger.application.book.result.BookMemberResult;
import com.spvermicelli.tripledger.ledger.application.book.result.CreateBookResult;
import com.spvermicelli.tripledger.ledger.application.book.result.TempParticipantResult;
import com.spvermicelli.tripledger.ledger.domain.book.model.Book;
import com.spvermicelli.tripledger.ledger.domain.book.model.BookMember;
import com.spvermicelli.tripledger.ledger.domain.book.repository.BookMemberRepository;
import com.spvermicelli.tripledger.ledger.domain.book.repository.BookRepository;
import com.spvermicelli.tripledger.ledger.domain.book.service.BookUserDirectory;
import com.spvermicelli.tripledger.ledger.domain.book.valueobject.BookUserSummary;
import com.spvermicelli.tripledger.ledger.domain.tempParticipant.model.TempParticipant;
import com.spvermicelli.tripledger.ledger.domain.tempParticipant.repository.TempParticipantRepository;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.spvermicelli.tripledger.shared.domain.enums.BookStatus;
import com.spvermicelli.tripledger.shared.domain.enums.MemberRole;
import com.spvermicelli.tripledger.shared.domain.enums.MemberStatus;
import com.spvermicelli.tripledger.shared.domain.enums.TempParticipantType;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 账本应用服务。
 * 负责账本用例编排、权限校验与跨仓储协作，不直接承担底层持久化细节。
 */
@Service
public class BookApplicationService {
    private static final int MAX_BOOK_NAME_LENGTH = 15;
    private static final int MAX_BOOK_DESCRIPTION_LENGTH = 30;

    private final BookRepository bookRepository;
    private final BookMemberRepository bookMemberRepository;
    private final TempParticipantRepository tempParticipantRepository;
    private final BookUserDirectory bookUserDirectory;

    public BookApplicationService(
        BookRepository bookRepository,
        BookMemberRepository bookMemberRepository,
        TempParticipantRepository tempParticipantRepository,
        BookUserDirectory bookUserDirectory
    ) {
        this.bookRepository = bookRepository;
        this.bookMemberRepository = bookMemberRepository;
        this.tempParticipantRepository = tempParticipantRepository;
        this.bookUserDirectory = bookUserDirectory;
    }

    @Transactional
    public CreateBookResult createBook(CreateBookCommand command) {
        String normalizedName = normalizeText(command.getName());
        String normalizedDescription = normalizeText(command.getDescription());
        String normalizedCoverUrl = normalizeText(command.getCoverUrl());
        validateCreateCommand(command, normalizedName, normalizedDescription);
        ensureBookNameUnique(command.getCurrentUserId(), normalizedName, null);

        Book savedBook = bookRepository.save(Book.builder()
            .name(normalizedName)
            .bookType(command.getBookType())
            .ownerUserId(command.getCurrentUserId())
            .description(normalizedDescription)
            .coverUrl(normalizedCoverUrl)
            .status(BookStatus.ACTIVE)
            .build());

        bookMemberRepository.save(BookMember.builder()
            .bookId(savedBook.getId())
            .userId(command.getCurrentUserId())
            .budgetAmountCent(0L)
            .memberRole(MemberRole.OWNER)
            .memberStatus(MemberStatus.ACTIVE)
            .joinedAt(LocalDateTime.now())
            .invitedByUserId(command.getCurrentUserId())
            .build());

        return CreateBookResult.builder()
            .bookId(savedBook.getId())
            .build();
    }

    /**
     * 获取我的账本列表
     * @param currentUserId token传入的userId
     * @return
     */
    @Transactional(readOnly = true)
    public List<BookListItemResult> getMyBooks(Long currentUserId) {
        // 验证当前 UserId 是否有效
        validateCurrentUser(currentUserId);
        List<Long> bookIds = bookMemberRepository.findActiveByUserId(currentUserId)
            .stream()
            .map(BookMember::getBookId)
            .distinct()
            .toList();

        return bookRepository.findByIds(bookIds)
            .stream()
            .filter(Book::isActive)
            .sorted(Comparator.comparing(Book::getUpdatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
            .map(book -> BookListItemResult.builder()
                .bookId(book.getId())
                .name(book.getName())
                .bookType(book.getBookType().getCode())
                .description(book.getDescription())
                .coverUrl(book.getCoverUrl())
                .createdAt(book.getCreatedAt())
                .build())
            .toList();
    }

    @Transactional(readOnly = true)
    public BookDetailResult getBookDetail(Long currentUserId, Long bookId) {
        validateCurrentUser(currentUserId);
        Book book = loadAccessibleBook(bookId, currentUserId);
        BookMember currentMember = bookMemberRepository.findByBookIdAndUserId(bookId, currentUserId)
            .filter(BookMember::isActive)
            .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "当前用户不在该账本中"));

        List<BookMember> members = bookMemberRepository.findByBookId(bookId);
        Map<Long, BookUserSummary> userSummaryMap = bookUserDirectory.getByUserIds(
            members.stream().map(BookMember::getUserId).collect(Collectors.toSet())
        );
        Map<Long, BookMember> memberMap = members.stream()
            .collect(Collectors.toMap(BookMember::getId, member -> member));

        List<BookMemberResult> memberResults = members.stream()
            .sorted(this::compareMemberForDetail)
            .map(member -> toMemberResult(member, userSummaryMap.get(member.getUserId())))
            .toList();

        List<TempParticipantResult> tempParticipantResults = book.isShared()
            ? tempParticipantRepository.findByBookId(bookId).stream()
                .filter(tempParticipant -> memberMap.containsKey(tempParticipant.getAttachedMemberId()))
                .filter(tempParticipant -> isTempVisibleToMember(tempParticipant, currentMember))
                .map(tempParticipant -> toTempParticipantResult(tempParticipant, memberMap, userSummaryMap))
                .toList()
            : Collections.emptyList();

        boolean canManageMembers = book.isShared() && currentMember.canManageMembers();
        boolean canInviteMembers = book.isShared() && currentMember.isActive();
        boolean canEditBook = !book.isShared() ? currentMember.isOwner() : currentMember.canManageMembers();

        return BookDetailResult.builder()
            .bookId(book.getId())
            .name(book.getName())
            .bookType(book.getBookType().getCode())
            .description(book.getDescription())
            .coverUrl(book.getCoverUrl())
            .ownerUserId(book.getOwnerUserId())
            .currentMemberId(currentMember.getId())
            .currentMemberRole(currentMember.getMemberRole().getCode())
            .currentMemberStatus(currentMember.getMemberStatus().getCode())
            .shared(book.isShared())
            .canEditBook(canEditBook)
            .canInviteMember(canInviteMembers)
            .canRemoveMember(canManageMembers)
            .canCancelAdmin(book.isShared() && currentMember.isOwner())
            .canTransferOwner(book.isShared() && currentMember.isOwner())
            .canQuitBook(book.isShared() && !currentMember.isOwner())
            .canDeleteBook(!book.isShared() && currentMember.isOwner())
            .members(memberResults)
            .tempParticipants(tempParticipantResults)
            .build();
    }

    @Transactional
    public void updateBook(UpdateBookCommand command) {
        String normalizedName = normalizeText(command.getName());
        String normalizedDescription = normalizeText(command.getDescription());
        String normalizedCoverUrl = normalizeText(command.getCoverUrl());
        validateUpdateCommand(command, normalizedName, normalizedDescription, normalizedCoverUrl);

        Book existingBook = loadBook(command.getBookId());
        BookMember operator = ensureActiveMembership(command.getBookId(), command.getCurrentUserId());
        if (!canEditBook(existingBook, operator)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户没有编辑该账本的权限");
        }
        Book updatedBook = existingBook.update(normalizedName, normalizedDescription, normalizedCoverUrl);
        if (!StringUtils.hasText(updatedBook.getName())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "账本名称不能为空");
        }
        validateBookName(updatedBook.getName());
        if (StringUtils.hasText(normalizedName)) {
            ensureBookNameUnique(existingBook.getOwnerUserId(), updatedBook.getName(), existingBook.getId());
        }
        if (StringUtils.hasText(normalizedDescription)) {
            validateBookDescription(normalizedDescription);
        }
        bookRepository.save(updatedBook);
    }

    /**
     * 当前先采用逻辑删除占位，避免在账单、成员、结算等删除链路尚未完成前直接做物理删除。
     * 后续真正实现全链路删除时，可在此用例中替换为聚合级联删除。
     */
    @Transactional
    public void deleteBook(DeleteBookCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        Book existingBook = loadOwnedBook(command.getBookId(), command.getCurrentUserId());
        bookRepository.save(Book.builder()
            .id(existingBook.getId())
            .name(existingBook.getName())
            .bookType(existingBook.getBookType())
            .ownerUserId(existingBook.getOwnerUserId())
            .description(existingBook.getDescription())
            .coverUrl(existingBook.getCoverUrl())
            .status(BookStatus.DELETED)
            .createdAt(existingBook.getCreatedAt())
            .updatedAt(existingBook.getUpdatedAt())
            .build());
    }

    private void validateCreateCommand(CreateBookCommand command, String normalizedName, String normalizedDescription) {
        validateCurrentUser(command.getCurrentUserId());
        if (!StringUtils.hasText(normalizedName)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "账本名称不能为空");
        }
        if (command.getBookType() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "bookType 不能为空");
        }
        validateBookName(normalizedName);
        validateBookDescription(normalizedDescription);
    }

    private void validateUpdateCommand(
        UpdateBookCommand command,
        String normalizedName,
        String normalizedDescription,
        String normalizedCoverUrl
    ) {
        validateCurrentUser(command.getCurrentUserId());
        if (command.getBookId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "bookId 不能为空");
        }
        if (!StringUtils.hasText(normalizedName)
            && !StringUtils.hasText(normalizedDescription)
            && !StringUtils.hasText(normalizedCoverUrl)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "name、description、coverUrl 不能同时为空");
        }
        if (StringUtils.hasText(normalizedName)) {
            validateBookName(normalizedName);
        }
        if (StringUtils.hasText(normalizedDescription)) {
            validateBookDescription(normalizedDescription);
        }
    }

    /**
     * 判断当前 UserId 不为空
     * @param currentUserId 传入的 UserId
     */
    private void validateCurrentUser(Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前登录信息不存在");
        }
    }

    private Book loadAccessibleBook(Long bookId, Long currentUserId) {
        Book book = loadBook(bookId);
        if (!book.isActive()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账本不存在");
        }
        Optional<BookMember> membership = bookMemberRepository.findByBookIdAndUserId(bookId, currentUserId)
            .filter(BookMember::isActive);
        if (membership.isEmpty()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户不在该账本中");
        }
        return book;
    }

    private Book loadOwnedBook(Long bookId, Long currentUserId) {
        Book book = loadAccessibleBook(bookId, currentUserId);
        if (!currentUserId.equals(book.getOwnerUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅账本创建者可以执行该操作");
        }
        return book;
    }

    private BookMember ensureActiveMembership(Long bookId, Long currentUserId) {
        return bookMemberRepository.findByBookIdAndUserId(bookId, currentUserId)
            .filter(BookMember::isActive)
            .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "当前用户不在该账本中"));
    }

    private Book loadBook(Long bookId) {
        if (bookId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "bookId 不能为空");
        }
        return bookRepository.findById(bookId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "账本不存在"));
    }

    private BookMemberResult toMemberResult(BookMember member, BookUserSummary userSummary) {
        return BookMemberResult.builder()
            .memberId(member.getId())
            .userId(member.getUserId())
            .nickname(userSummary == null ? null : userSummary.getNickname())
            .avatarUrl(userSummary == null ? null : userSummary.getAvatarUrl())
            .phoneNumber(userSummary == null ? null : userSummary.getPhoneNumber())
            .memberRole(member.getMemberRole().getCode())
            .memberStatus(member.getMemberStatus().getCode())
            .joinedAt(member.getJoinedAt())
            .leftAt(member.getLeftAt())
            .build();
    }

    private TempParticipantResult toTempParticipantResult(
        TempParticipant tempParticipant,
        Map<Long, BookMember> memberMap,
        Map<Long, BookUserSummary> userSummaryMap
    ) {
        BookMember attachedMember = memberMap.get(tempParticipant.getAttachedMemberId());
        BookUserSummary attachedUser = attachedMember == null ? null : userSummaryMap.get(attachedMember.getUserId());
        return TempParticipantResult.builder()
            .tempParticipantId(tempParticipant.getId())
            .nickname(tempParticipant.getNickname())
            .tempType(TempParticipantType.safeCode(tempParticipant.getTempType()))
            .status(tempParticipant.getStatus() == null ? null : tempParticipant.getStatus().getCode())
            .attachedMemberId(tempParticipant.getAttachedMemberId())
            .attachedUserId(attachedMember == null ? null : attachedMember.getUserId())
            .attachedNickname(attachedUser == null ? null : attachedUser.getNickname())
            .attachedAvatarUrl(attachedUser == null ? null : attachedUser.getAvatarUrl())
            .attachedPhoneNumber(attachedUser == null ? null : attachedUser.getPhoneNumber())
            .build();
    }

    private boolean isTempVisibleToMember(TempParticipant tempParticipant, BookMember currentMember) {
        if (tempParticipant.isGlobal()) {
            return true;
        }
        return Objects.equals(tempParticipant.getAttachedMemberId(), currentMember.getId());
    }

    private String normalizeText(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private void validateBookName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "账本名称不能为空");
        }
        if (name.trim().length() > MAX_BOOK_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "账本名称不能超过15个字");
        }
    }

    private void validateBookDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return;
        }
        if (description.trim().length() > MAX_BOOK_DESCRIPTION_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "账本描述不能超过30个字");
        }
    }

    private void ensureBookNameUnique(Long ownerUserId, String normalizedName, Long excludeBookId) {
        if (!StringUtils.hasText(normalizedName)) {
            return;
        }
        boolean exists = bookRepository.existsByOwnerUserIdAndName(ownerUserId, normalizedName, excludeBookId);
        if (exists) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前账号下已存在同名账本");
        }
    }

    private boolean canEditBook(Book book, BookMember member) {
        if (!book.isShared()) {
            return member.isOwner();
        }
        return member.canManageMembers();
    }

    private int compareMemberForDetail(BookMember left, BookMember right) {
        int statusCompare = Integer.compare(memberStatusWeight(left), memberStatusWeight(right));
        if (statusCompare != 0) {
            return statusCompare;
        }

        int roleCompare = Integer.compare(memberRoleWeight(left), memberRoleWeight(right));
        if (roleCompare != 0) {
            return roleCompare;
        }

        LocalDateTime leftTime = left.isActive() ? left.getJoinedAt() : left.getLeftAt();
        LocalDateTime rightTime = right.isActive() ? right.getJoinedAt() : right.getLeftAt();
        if (leftTime == null && rightTime == null) {
            return Long.compare(left.getId(), right.getId());
        }
        if (leftTime == null) {
            return 1;
        }
        if (rightTime == null) {
            return -1;
        }
        return rightTime.compareTo(leftTime);
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
