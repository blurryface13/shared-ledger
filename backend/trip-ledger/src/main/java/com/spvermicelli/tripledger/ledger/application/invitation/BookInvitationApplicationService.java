package com.spvermicelli.tripledger.ledger.application.invitation;

import com.spvermicelli.tripledger.ledger.application.invitation.command.AcceptInvitationCommand;
import com.spvermicelli.tripledger.ledger.application.invitation.command.CreateInvitationCommand;
import com.spvermicelli.tripledger.ledger.application.invitation.command.RejectInvitationCommand;
import com.spvermicelli.tripledger.ledger.application.invitation.command.RevokeInvitationCommand;
import com.spvermicelli.tripledger.ledger.application.invitation.result.CreateInvitationResult;
import com.spvermicelli.tripledger.ledger.application.invitation.result.InvitationCandidateResult;
import com.spvermicelli.tripledger.ledger.application.invitation.result.InvitationMessageItemResult;
import com.spvermicelli.tripledger.ledger.application.invitation.result.InvitationOperationResult;
import com.spvermicelli.tripledger.ledger.application.invitation.result.PendingInvitationResult;
import com.spvermicelli.tripledger.ledger.domain.book.model.Book;
import com.spvermicelli.tripledger.ledger.domain.book.model.BookMember;
import com.spvermicelli.tripledger.ledger.domain.book.repository.BookMemberRepository;
import com.spvermicelli.tripledger.ledger.domain.book.repository.BookRepository;
import com.spvermicelli.tripledger.ledger.domain.book.service.BookUserDirectory;
import com.spvermicelli.tripledger.ledger.domain.book.valueobject.BookUserSummary;
import com.spvermicelli.tripledger.ledger.domain.invitation.model.BookInvitation;
import com.spvermicelli.tripledger.ledger.domain.invitation.repository.BookInvitationRepository;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.spvermicelli.tripledger.shared.domain.enums.InvitationStatus;
import com.spvermicelli.tripledger.shared.domain.enums.MemberRole;
import com.spvermicelli.tripledger.shared.domain.enums.MemberStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 账本邀请应用服务。
 * 邀请链路的关键目标是：在后端自行确认操作者身份、目标用户有效性、账本成员关系和邀请状态机，
 * 避免前端通过伪造 userId、memberRole 或邀请状态实现越权。
 */
@Service
public class BookInvitationApplicationService {

    private final BookRepository bookRepository;
    private final BookMemberRepository bookMemberRepository;
    private final BookInvitationRepository bookInvitationRepository;
    private final BookUserDirectory bookUserDirectory;

    public BookInvitationApplicationService(
        BookRepository bookRepository,
        BookMemberRepository bookMemberRepository,
        BookInvitationRepository bookInvitationRepository,
        BookUserDirectory bookUserDirectory
    ) {
        this.bookRepository = bookRepository;
        this.bookMemberRepository = bookMemberRepository;
        this.bookInvitationRepository = bookInvitationRepository;
        this.bookUserDirectory = bookUserDirectory;
    }

    @Transactional
    public CreateInvitationResult createInvitation(CreateInvitationCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        validateInviteRequest(command);

        Book book = loadBook(command.getBookId());
        ensureSharedBook(book);
        BookMember operator = ensureActiveMembership(book.getId(), command.getCurrentUserId());
        ensureCanInviteMembers(operator);

        if (command.getCurrentUserId().equals(command.getInviteeUserId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "不能邀请自己加入当前账本");
        }
        bookUserDirectory.getActiveUser(command.getInviteeUserId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "被邀请用户不存在或账号已失效"));

        bookMemberRepository.findByBookIdAndUserId(book.getId(), command.getInviteeUserId())
            .filter(BookMember::isActive)
            .ifPresent(member -> {
                throw new BusinessException(ErrorCode.CONFLICT, "该用户已经在账本中");
            });

        LocalDateTime now = LocalDateTime.now();
        bookInvitationRepository.findPendingByBookIdAndInviteeUserId(book.getId(), command.getInviteeUserId())
            .ifPresent(existingInvitation -> {
                if (existingInvitation.isExpired(now)) {
                    bookInvitationRepository.save(existingInvitation.expire(now));
                    return;
                }
                throw new BusinessException(ErrorCode.CONFLICT, "该用户已有一条待处理邀请，请勿重复邀请");
            });

        BookInvitation savedInvitation = bookInvitationRepository.save(BookInvitation.builder()
            .bookId(book.getId())
            .inviterUserId(command.getCurrentUserId())
            .inviteeUserId(command.getInviteeUserId())
            .status(InvitationStatus.PENDING)
            .invitedAt(now)
            .remark(normalizeRemark(command.getRemark()))
            .build());

        return CreateInvitationResult.builder()
            .invitationId(savedInvitation.getId())
            .status(savedInvitation.getStatus().getCode())
            .expireAt(savedInvitation.getExpireAt())
            .message("邀请已发送，对方需在 24 小时内处理")
            .build();
    }

    @Transactional(readOnly = true)
    public List<InvitationCandidateResult> searchInvitationCandidates(
        Long currentUserId,
        Long bookId,
        String searchType,
        String keyword
    ) {
        validateCurrentUser(currentUserId);
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }

        Book book = loadBook(bookId);
        ensureSharedBook(book);
        BookMember operator = ensureActiveMembership(book.getId(), currentUserId);
        ensureCanInviteMembers(operator);
        if (!"MOBILE".equalsIgnoreCase(searchType) && !"NICKNAME".equalsIgnoreCase(searchType)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "searchType 仅支持 MOBILE 或 NICKNAME");
        }

        String normalizedKeyword = keyword.trim();
        List<BookUserSummary> candidates = "MOBILE".equalsIgnoreCase(searchType)
            ? bookUserDirectory.searchActiveUsersByPhoneKeyword(normalizedKeyword)
            : bookUserDirectory.searchActiveUsersByNicknameKeyword(normalizedKeyword);

        if (candidates.isEmpty()) {
            return List.of();
        }

        Map<Long, BookMember> activeMemberMap = bookMemberRepository.findActiveByBookId(bookId)
            .stream()
            .collect(Collectors.toMap(BookMember::getUserId, member -> member, (left, right) -> left));
        Map<Long, BookInvitation> pendingInvitationMap = bookInvitationRepository.findPendingByBookIdAndInviteeUserIds(
                bookId,
                candidates.stream().map(BookUserSummary::getUserId).collect(Collectors.toSet())
            ).stream()
            .collect(Collectors.toMap(BookInvitation::getInviteeUserId, invitation -> invitation, (left, right) -> left));

        return candidates.stream()
            .filter(candidate -> !candidate.getUserId().equals(currentUserId))
            .filter(candidate -> !activeMemberMap.containsKey(candidate.getUserId()))
            .map(candidate -> InvitationCandidateResult.builder()
                .userId(candidate.getUserId())
                .nickname(candidate.getNickname())
                .avatarUrl(candidate.getAvatarUrl())
                .phoneNumber(candidate.getPhoneNumber())
                .pendingInvitation(pendingInvitationMap.containsKey(candidate.getUserId()))
                .build())
            .toList();
    }

    @Transactional
    public InvitationOperationResult acceptInvitation(AcceptInvitationCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        BookInvitation invitation = loadOwnedPendingInvitation(command.getInvitationId(), command.getCurrentUserId());
        LocalDateTime now = LocalDateTime.now();
        if (invitation.isExpired(now)) {
            bookInvitationRepository.save(invitation.expire(now));
            throw new BusinessException(ErrorCode.INVALID_STATUS, "邀请已过期，请让对方重新发起邀请");
        }

        Book book = loadBook(invitation.getBookId());
        ensureSharedBook(book);

        BookMember existingMember = bookMemberRepository.findByBookIdAndUserId(book.getId(), command.getCurrentUserId())
            .orElse(null);

        if (existingMember != null && existingMember.isActive()) {
            bookInvitationRepository.save(invitation.accept(now));
            return InvitationOperationResult.builder()
                .invitationId(invitation.getId())
                .status(InvitationStatus.ACCEPTED.getCode())
                .message("你已经在该账本中，本次邀请已自动处理")
                .build();
        }

        if (existingMember == null) {
            bookMemberRepository.save(BookMember.builder()
                .bookId(book.getId())
                .userId(command.getCurrentUserId())
                .budgetAmountCent(0L)
                .memberRole(MemberRole.MEMBER)
                .memberStatus(MemberStatus.ACTIVE)
                .joinedAt(now)
                .leftAt(null)
                .invitedByUserId(invitation.getInviterUserId())
                .build());
        } else {
            bookMemberRepository.save(existingMember.reactivate(invitation.getInviterUserId(), now));
        }

        bookInvitationRepository.save(invitation.accept(now));
        return InvitationOperationResult.builder()
            .invitationId(invitation.getId())
            .status(InvitationStatus.ACCEPTED.getCode())
            .message("你已成功加入账本")
            .build();
    }

    @Transactional
    public InvitationOperationResult rejectInvitation(RejectInvitationCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        BookInvitation invitation = loadOwnedPendingInvitation(command.getInvitationId(), command.getCurrentUserId());
        LocalDateTime now = LocalDateTime.now();
        if (invitation.isExpired(now)) {
            bookInvitationRepository.save(invitation.expire(now));
            throw new BusinessException(ErrorCode.INVALID_STATUS, "邀请已过期，请让对方重新发起邀请");
        }

        bookInvitationRepository.save(invitation.reject(now));
        return InvitationOperationResult.builder()
            .invitationId(invitation.getId())
            .status(InvitationStatus.REJECTED.getCode())
            .message("你已拒绝该邀请")
            .build();
    }

    @Transactional
    public InvitationOperationResult revokeInvitation(RevokeInvitationCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        if (command.getInvitationId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "invitationId 不能为空");
        }

        BookInvitation invitation = bookInvitationRepository.findById(command.getInvitationId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "邀请不存在"));
        if (!invitation.getInviterUserId().equals(command.getCurrentUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无权撤销该邀请");
        }
        if (!invitation.isPending()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, buildHandledMessage(invitation.getStatus()));
        }

        LocalDateTime now = LocalDateTime.now();
        if (invitation.isExpired(now)) {
            bookInvitationRepository.save(invitation.expire(now));
            throw new BusinessException(ErrorCode.INVALID_STATUS, "邀请已过期，请勿重复处理");
        }

        Book book = loadBook(invitation.getBookId());
        ensureSharedBook(book);
        BookMember operator = ensureActiveMembership(book.getId(), command.getCurrentUserId());
        ensureCanInviteMembers(operator);

        bookInvitationRepository.save(invitation.revoke(now));
        return InvitationOperationResult.builder()
            .invitationId(invitation.getId())
            .status(InvitationStatus.REVOKED.getCode())
            .message("邀请已撤销")
            .build();
    }

    @Transactional
    public List<InvitationMessageItemResult> getReceivedInvitations(Long currentUserId) {
        validateCurrentUser(currentUserId);
        LocalDateTime now = LocalDateTime.now();
        List<BookInvitation> invitations = bookInvitationRepository.findByInviteeUserId(currentUserId)
            .stream()
            .map(invitation -> normalizeInvitationStatus(invitation, now))
            .toList();
        return toInvitationMessageResults(currentUserId, invitations, false);
    }

    @Transactional
    public List<InvitationMessageItemResult> getSentInvitations(Long currentUserId) {
        validateCurrentUser(currentUserId);
        LocalDateTime now = LocalDateTime.now();
        List<BookInvitation> invitations = bookInvitationRepository.findByInviterUserId(currentUserId)
            .stream()
            .map(invitation -> normalizeInvitationStatus(invitation, now))
            .toList();
        return toInvitationMessageResults(currentUserId, invitations, true);
    }

    @Transactional
    public List<PendingInvitationResult> getPendingInvitations(Long currentUserId) {
        validateCurrentUser(currentUserId);
        LocalDateTime now = LocalDateTime.now();

        List<BookInvitation> validInvitations = bookInvitationRepository.findPendingByInviteeUserId(currentUserId)
            .stream()
            .map(invitation -> {
                if (invitation.isExpired(now)) {
                    bookInvitationRepository.save(invitation.expire(now));
                    return null;
                }
                return invitation;
            })
            .filter(java.util.Objects::nonNull)
            .toList();

        if (validInvitations.isEmpty()) {
            return List.of();
        }

        Map<Long, Book> bookMap = bookRepository.findByIds(
                validInvitations.stream().map(BookInvitation::getBookId).collect(Collectors.toSet())
            ).stream()
            .filter(Book::isActive)
            .collect(Collectors.toMap(Book::getId, book -> book));

        Map<Long, BookUserSummary> inviterSummaryMap = bookUserDirectory.getByUserIds(
            validInvitations.stream().map(BookInvitation::getInviterUserId).collect(Collectors.toSet())
        );

        return validInvitations.stream()
            .filter(invitation -> bookMap.containsKey(invitation.getBookId()))
            .map(invitation -> {
                Book book = bookMap.get(invitation.getBookId());
                BookUserSummary inviterSummary = inviterSummaryMap.get(invitation.getInviterUserId());
                return PendingInvitationResult.builder()
                    .invitationId(invitation.getId())
                    .bookId(invitation.getBookId())
                    .bookName(book == null ? null : book.getName())
                    .inviterUserId(invitation.getInviterUserId())
                    .inviterNickname(inviterSummary == null ? null : inviterSummary.getNickname())
                    .inviterAvatarUrl(inviterSummary == null ? null : inviterSummary.getAvatarUrl())
                    .status(invitation.getStatus().getCode())
                    .invitedAt(invitation.getInvitedAt())
                    .expireAt(invitation.getExpireAt())
                    .remark(invitation.getRemark())
                    .build();
            })
            .toList();
    }

    private void validateCurrentUser(Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前登录信息不存在");
        }
    }

    private void validateInviteRequest(CreateInvitationCommand command) {
        if (command.getBookId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "bookId 不能为空");
        }
        if (command.getInviteeUserId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "inviteeUserId 不能为空");
        }
        if (StringUtils.hasText(command.getRemark()) && command.getRemark().trim().length() > 50) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "备注长度不能超过 50 个字符");
        }
    }

    private Book loadBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "账本不存在"));
        if (!book.isActive()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账本不存在");
        }
        return book;
    }

    private void ensureSharedBook(Book book) {
        if (!book.isShared()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "个人账本不支持邀请成员");
        }
    }

    private BookMember ensureActiveMembership(Long bookId, Long currentUserId) {
        return bookMemberRepository.findByBookIdAndUserId(bookId, currentUserId)
            .filter(BookMember::isActive)
            .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "当前用户不在该账本中"));
    }

    private void ensureCanInviteMembers(BookMember operator) {
        if (operator == null || !operator.isActive()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户不在该账本中");
        }
    }

    private BookInvitation loadOwnedPendingInvitation(Long invitationId, Long currentUserId) {
        if (invitationId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "invitationId 不能为空");
        }
        BookInvitation invitation = bookInvitationRepository.findById(invitationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "邀请不存在"));
        if (!invitation.getInviteeUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无权处理该邀请");
        }
        if (!invitation.isPending()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, buildHandledMessage(invitation.getStatus()));
        }
        return invitation;
    }

    private BookInvitation normalizeInvitationStatus(BookInvitation invitation, LocalDateTime now) {
        if (invitation.isExpired(now)) {
            return bookInvitationRepository.save(invitation.expire(now));
        }
        return invitation;
    }

    private List<InvitationMessageItemResult> toInvitationMessageResults(
        Long currentUserId,
        List<BookInvitation> invitations,
        boolean sentView
    ) {
        if (invitations.isEmpty()) {
            return List.of();
        }

        Map<Long, Book> bookMap = bookRepository.findByIds(
                invitations.stream().map(BookInvitation::getBookId).collect(Collectors.toSet())
            ).stream()
            .filter(Book::isActive)
            .collect(Collectors.toMap(Book::getId, book -> book));

        Set<Long> relatedUserIds = invitations.stream()
            .flatMap(invitation -> java.util.stream.Stream.of(invitation.getInviterUserId(), invitation.getInviteeUserId()))
            .collect(Collectors.toSet());
        Map<Long, BookUserSummary> userSummaryMap = bookUserDirectory.getByUserIds(relatedUserIds);

        Set<Long> revocableBookIds = sentView
            ? bookMemberRepository.findActiveByUserId(currentUserId).stream()
                .map(BookMember::getBookId)
                .collect(Collectors.toSet())
            : Set.of();

        return invitations.stream()
            .filter(invitation -> bookMap.containsKey(invitation.getBookId()))
            .map(invitation -> {
                Book book = bookMap.get(invitation.getBookId());
                BookUserSummary inviter = userSummaryMap.get(invitation.getInviterUserId());
                BookUserSummary invitee = userSummaryMap.get(invitation.getInviteeUserId());
                boolean canRevoke = sentView
                    && invitation.isPending()
                    && invitation.getInviterUserId().equals(currentUserId)
                    && revocableBookIds.contains(invitation.getBookId());
                return InvitationMessageItemResult.builder()
                    .invitationId(invitation.getId())
                    .bookId(invitation.getBookId())
                    .bookName(book == null ? null : book.getName())
                    .bookCoverUrl(book == null ? null : book.getCoverUrl())
                    .status(invitation.getStatus().getCode())
                    .invitedAt(invitation.getInvitedAt())
                    .handledAt(invitation.getHandledAt())
                    .expireAt(invitation.getExpireAt())
                    .remark(invitation.getRemark())
                    .inviterUserId(invitation.getInviterUserId())
                    .inviterNickname(inviter == null ? null : inviter.getNickname())
                    .inviterAvatarUrl(inviter == null ? null : inviter.getAvatarUrl())
                    .inviterPhoneNumber(inviter == null ? null : inviter.getPhoneNumber())
                    .inviteeUserId(invitation.getInviteeUserId())
                    .inviteeNickname(invitee == null ? null : invitee.getNickname())
                    .inviteeAvatarUrl(invitee == null ? null : invitee.getAvatarUrl())
                    .inviteePhoneNumber(invitee == null ? null : invitee.getPhoneNumber())
                    .canRevoke(canRevoke)
                    .build();
            })
            .toList();
    }

    private String buildHandledMessage(InvitationStatus status) {
        return switch (status) {
            case ACCEPTED -> "该邀请已被接受，请勿重复处理";
            case REJECTED -> "该邀请已被拒绝，请勿重复处理";
            case EXPIRED -> "该邀请已过期，请让对方重新发起邀请";
            case REVOKED -> "该邀请已被撤销，请勿重复处理";
            case PENDING -> "邀请状态异常";
        };
    }

    private String normalizeRemark(String remark) {
        return StringUtils.hasText(remark) ? remark.trim() : null;
    }
}
