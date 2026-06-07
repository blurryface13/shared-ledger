package com.spvermicelli.tripledger.ledger.application.tempParticipant;

import com.spvermicelli.tripledger.ledger.application.tempParticipant.command.ChangeTempParticipantAttachedMemberCommand;
import com.spvermicelli.tripledger.ledger.application.tempParticipant.command.CreateTempParticipantCommand;
import com.spvermicelli.tripledger.ledger.application.tempParticipant.command.DeleteTempParticipantCommand;
import com.spvermicelli.tripledger.ledger.application.tempParticipant.command.EnableTempParticipantCommand;
import com.spvermicelli.tripledger.ledger.application.tempParticipant.command.UpdateTempParticipantNicknameCommand;
import com.spvermicelli.tripledger.ledger.application.tempParticipant.result.TempParticipantDetailResult;
import com.spvermicelli.tripledger.ledger.application.tempParticipant.result.TempParticipantMemberResult;
import com.spvermicelli.tripledger.ledger.application.tempParticipant.result.TempParticipantOperationResult;
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
import com.spvermicelli.tripledger.shared.domain.enums.TempParticipantStatus;
import com.spvermicelli.tripledger.shared.domain.enums.TempParticipantType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 临时成员应用服务。
 * 该服务负责：
 * 1. 根据当前 token 身份校验用户是否属于目标账本；
 * 2. 将“可见性、可修改性、可删除性”全部收回后端独立判定；
 * 3. 统一处理临时成员与正式成员之间的挂靠关系约束。
 *
 * 说明：
 * V3 调整为“按临时成员类型收敛可见性”：
 * 1. PRIVATE 仅挂靠正式成员可见，只能用于个人帮带；
 * 2. GLOBAL 全账本可见，可用于个人帮带与共享账单；
 * 3. 临时成员的改名/停用统一由创建者维护，PRIVATE 类型不允许改挂靠。
 */
@Service
public class TempParticipantApplicationService {

    private final BookRepository bookRepository;
    private final BookMemberRepository bookMemberRepository;
    private final TempParticipantRepository tempParticipantRepository;
    private final BookUserDirectory bookUserDirectory;

    public TempParticipantApplicationService(
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

    @Transactional(readOnly = true)
    public List<TempParticipantDetailResult> getTempParticipants(Long currentUserId, Long bookId, TempParticipantType tempType) {
        validateCurrentUser(currentUserId);
        BookMember currentMember = requireCurrentActiveMember(bookId, currentUserId);
        TempParticipantType normalizedFilterType = tempType == null ? null : TempParticipantType.normalize(tempType);

        List<TempParticipant> visibleParticipants = tempParticipantRepository.findByBookId(bookId).stream()
            .filter(tempParticipant -> isVisibleToMember(tempParticipant, currentMember))
            .filter(tempParticipant -> normalizedFilterType == null
                || TempParticipantType.normalize(tempParticipant.getTempType()) == normalizedFilterType)
            .toList();

        Map<Long, BookMember> memberMap = loadRelatedMembers(bookId, visibleParticipants);
        Map<Long, BookUserSummary> userSummaryMap = loadRelatedUsers(memberMap.values());

        return visibleParticipants.stream()
            .map(tempParticipant -> toDetailResult(tempParticipant, memberMap, userSummaryMap))
            .toList();
    }

    @Transactional
    public TempParticipantOperationResult createTempParticipant(CreateTempParticipantCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        validateCreateCommand(command);

        BookMember currentMember = requireCurrentActiveMember(command.getBookId(), command.getCurrentUserId());
        TempParticipantType normalizedType = requireTempType(command.getTempType());
        Long attachedMemberId = normalizedType == TempParticipantType.PRIVATE
            ? currentMember.getId()
            : command.getAttachedMemberId();
        BookMember attachedMember = requireActiveMemberById(command.getBookId(), attachedMemberId,
            "不存在指定的挂靠成员");

        ensureNicknameAvailable(command.getBookId(), command.getNickname(), null);

        TempParticipant savedParticipant = tempParticipantRepository.save(TempParticipant.builder()
            .bookId(command.getBookId())
            .nickname(command.getNickname().trim())
            .tempType(normalizedType)
            .createdByMemberId(currentMember.getId())
            .attachedMemberId(attachedMember.getId())
            .status(TempParticipantStatus.ACTIVE)
            .build());

        return TempParticipantOperationResult.builder()
            .tempParticipantId(savedParticipant.getId())
            .message("临时成员创建成功")
            .build();
    }

    @Transactional
    public TempParticipantOperationResult updateNickname(UpdateTempParticipantNicknameCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        if (command.getTempParticipantId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "tempParticipantId 不能为空");
        }
        if (!StringUtils.hasText(command.getNickname())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "新的临时成员昵称不能为空");
        }

        BookMember currentMember = requireCurrentActiveMember(command.getBookId(), command.getCurrentUserId());
        TempParticipant tempParticipant = requireActiveTempParticipant(command.getBookId(), command.getTempParticipantId());
        ensureCreatorOperator(tempParticipant, currentMember);
        ensureNicknameAvailable(command.getBookId(), command.getNickname(), tempParticipant.getId());

        tempParticipantRepository.save(tempParticipant.rename(command.getNickname()));
        return TempParticipantOperationResult.builder()
            .tempParticipantId(tempParticipant.getId())
            .message("临时成员昵称更新成功")
            .build();
    }

    @Transactional
    public TempParticipantOperationResult changeAttachedMember(ChangeTempParticipantAttachedMemberCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        if (command.getTempParticipantId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "tempParticipantId 不能为空");
        }
        if (command.getAttachedMemberId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "新的挂靠正式成员不能为空");
        }

        BookMember currentMember = requireCurrentActiveMember(command.getBookId(), command.getCurrentUserId());
        TempParticipant tempParticipant = requireActiveTempParticipant(command.getBookId(), command.getTempParticipantId());
        ensureCreatorOperator(tempParticipant, currentMember);
        if (TempParticipantType.normalize(tempParticipant.getTempType()) == TempParticipantType.PRIVATE) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "私有临时成员不允许改挂靠");
        }

        BookMember targetAttachedMember = requireActiveMemberById(command.getBookId(), command.getAttachedMemberId(),
            "不存在指定的挂靠成员");
        if (Objects.equals(tempParticipant.getAttachedMemberId(), targetAttachedMember.getId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "新的挂靠正式成员与当前挂靠正式成员一致，无需重复修改");
        }

        tempParticipantRepository.save(tempParticipant.changeAttachedMember(targetAttachedMember.getId()));
        return TempParticipantOperationResult.builder()
            .tempParticipantId(tempParticipant.getId())
            .message("挂靠正式成员更新成功")
            .build();
    }

    @Transactional
    public TempParticipantOperationResult deleteTempParticipant(DeleteTempParticipantCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        if (command.getTempParticipantId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "tempParticipantId 不能为空");
        }

        BookMember currentMember = requireCurrentActiveMember(command.getBookId(), command.getCurrentUserId());
        TempParticipant tempParticipant = requireActiveTempParticipant(command.getBookId(), command.getTempParticipantId());
        ensureCreatorOperator(tempParticipant, currentMember);

        tempParticipantRepository.save(tempParticipant.disable());
        return TempParticipantOperationResult.builder()
            .tempParticipantId(tempParticipant.getId())
            .message("临时成员已禁用")
            .build();
    }

    @Transactional
    public TempParticipantOperationResult enableTempParticipant(EnableTempParticipantCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        if (command.getTempParticipantId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "tempParticipantId 不能为空");
        }

        BookMember currentMember = requireCurrentActiveMember(command.getBookId(), command.getCurrentUserId());
        TempParticipant tempParticipant = requireTempParticipant(command.getBookId(), command.getTempParticipantId());
        ensureCreatorOperator(tempParticipant, currentMember);
        if (tempParticipant.isActive()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "该临时成员当前已启用，无需重复启用");
        }
        requireActiveMemberById(command.getBookId(), tempParticipant.getAttachedMemberId(), "不存在指定的挂靠成员");

        tempParticipantRepository.save(tempParticipant.enable());
        return TempParticipantOperationResult.builder()
            .tempParticipantId(tempParticipant.getId())
            .message("临时成员已启用")
            .build();
    }

    private void validateCurrentUser(Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前登录信息不存在");
        }
    }

    private void validateCreateCommand(CreateTempParticipantCommand command) {
        if (command.getBookId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "bookId 不能为空");
        }
        if (!StringUtils.hasText(command.getNickname())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "临时成员昵称不能为空");
        }
        if (command.getTempType() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "tempType 不能为空");
        }
        TempParticipantType normalizedType = TempParticipantType.normalize(command.getTempType());
        if (normalizedType == TempParticipantType.GLOBAL && command.getAttachedMemberId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "attachedMemberId 不能为空");
        }
    }

    private TempParticipantType requireTempType(TempParticipantType tempType) {
        if (tempType == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "tempType 仅支持 PRIVATE 或 GLOBAL");
        }
        return TempParticipantType.normalize(tempType);
    }

    private BookMember requireCurrentActiveMember(Long bookId, Long currentUserId) {
        loadActiveBook(bookId);
        return bookMemberRepository.findByBookIdAndUserId(bookId, currentUserId)
            .filter(BookMember::isActive)
            .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "权限不足，您不在该账本内"));
    }

    private BookMember requireActiveMemberById(Long bookId, Long memberId, String errorMessage) {
        return bookMemberRepository.findById(memberId)
            .filter(BookMember::isActive)
            .filter(member -> Objects.equals(member.getBookId(), bookId))
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, errorMessage));
    }

    private TempParticipant requireActiveTempParticipant(Long bookId, Long tempParticipantId) {
        TempParticipant tempParticipant = requireTempParticipant(bookId, tempParticipantId);
        if (!tempParticipant.isActive()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "该临时成员当前已禁用，无法继续操作");
        }
        return tempParticipant;
    }

    private TempParticipant requireTempParticipant(Long bookId, Long tempParticipantId) {
        return tempParticipantRepository.findById(tempParticipantId)
            .filter(item -> Objects.equals(item.getBookId(), bookId))
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "临时成员不存在"));
    }

    private void ensureCreatorOperator(TempParticipant tempParticipant, BookMember currentMember) {
        if (!Objects.equals(tempParticipant.getCreatedByMemberId(), currentMember.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有临时成员创建者可以操作该临时成员");
        }
    }

    private void ensureNicknameAvailable(Long bookId, String nickname, Long currentTempParticipantId) {
        Optional<TempParticipant> existing = tempParticipantRepository.findByBookIdAndNickname(bookId, nickname.trim());
        if (existing.isEmpty() || Objects.equals(existing.get().getId(), currentTempParticipantId)) {
            return;
        }

        if (existing.get().isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前账本中不允许创建重复昵称的临时成员");
        }
        throw new BusinessException(ErrorCode.INVALID_STATUS, "该临时成员已存在但状态为禁用，请重新启用后使用");
    }

    private boolean isVisibleToMember(TempParticipant tempParticipant, BookMember currentMember) {
        if (tempParticipant.isGlobal()) {
            return true;
        }
        return Objects.equals(tempParticipant.getAttachedMemberId(), currentMember.getId());
    }

    private Map<Long, BookMember> loadRelatedMembers(Long bookId, List<TempParticipant> tempParticipants) {
        Map<Long, BookMember> activeMemberMap = bookMemberRepository.findActiveByBookId(bookId).stream()
            .collect(Collectors.toMap(BookMember::getId, member -> member, (left, right) -> left));

        tempParticipants.stream()
            .flatMap(tempParticipant -> java.util.stream.Stream.of(
                tempParticipant.getCreatedByMemberId(),
                tempParticipant.getAttachedMemberId()
            ))
            .filter(Objects::nonNull)
            .distinct()
            .forEach(memberId -> {
                if (!activeMemberMap.containsKey(memberId)) {
                    bookMemberRepository.findById(memberId).ifPresent(member -> activeMemberMap.put(memberId, member));
                }
            });
        return activeMemberMap;
    }

    private Map<Long, BookUserSummary> loadRelatedUsers(Collection<BookMember> members) {
        return bookUserDirectory.getByUserIds(members.stream()
            .map(BookMember::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .toList());
    }

    private TempParticipantDetailResult toDetailResult(
        TempParticipant tempParticipant,
        Map<Long, BookMember> memberMap,
        Map<Long, BookUserSummary> userSummaryMap
    ) {
        return TempParticipantDetailResult.builder()
            .tempParticipantId(tempParticipant.getId())
            .nickname(tempParticipant.getNickname())
            .tempType(TempParticipantType.safeCode(tempParticipant.getTempType()))
            .status(tempParticipant.getStatus().getCode())
            .createdByMember(toMemberResult(memberMap.get(tempParticipant.getCreatedByMemberId()), userSummaryMap))
            .attachedMember(toMemberResult(memberMap.get(tempParticipant.getAttachedMemberId()), userSummaryMap))
            .createdAt(tempParticipant.getCreatedAt())
            .updatedAt(tempParticipant.getUpdatedAt())
            .build();
    }

    private TempParticipantMemberResult toMemberResult(BookMember member, Map<Long, BookUserSummary> userSummaryMap) {
        if (member == null) {
            return null;
        }
        BookUserSummary summary = userSummaryMap.get(member.getUserId());
        return TempParticipantMemberResult.builder()
            .memberId(member.getId())
            .userId(member.getUserId())
            .nickname(summary == null ? null : summary.getNickname())
            .avatarUrl(summary == null ? null : summary.getAvatarUrl())
            .phoneNumber(summary == null ? null : summary.getPhoneNumber())
            .build();
    }

    private Book loadActiveBook(Long bookId) {
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
}
