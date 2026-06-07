package com.spvermicelli.tripledger.settlement.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spvermicelli.tripledger.billing.application.statistics.StatisticsApplicationService;
import com.spvermicelli.tripledger.billing.application.statistics.result.MemberRelationResult;
import com.spvermicelli.tripledger.billing.application.support.BillingAccessSupportService;
import com.spvermicelli.tripledger.billing.application.support.BillingBookMemberContext;
import com.spvermicelli.tripledger.ledger.domain.book.model.BookMember;
import com.spvermicelli.tripledger.ledger.domain.book.repository.BookMemberRepository;
import com.spvermicelli.tripledger.ledger.domain.book.service.BookUserDirectory;
import com.spvermicelli.tripledger.ledger.domain.book.valueobject.BookUserSummary;
import com.spvermicelli.tripledger.settlement.application.command.CreateSettlementCommand;
import com.spvermicelli.tripledger.settlement.application.result.SettlementBatchResult;
import com.spvermicelli.tripledger.settlement.application.result.SettlementTransferResult;
import com.spvermicelli.tripledger.settlement.domain.model.SettlementBatch;
import com.spvermicelli.tripledger.settlement.domain.model.SettlementTransfer;
import com.spvermicelli.tripledger.settlement.domain.repository.SettlementBatchRepository;
import com.spvermicelli.tripledger.settlement.domain.repository.SettlementTransferRepository;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.spvermicelli.tripledger.shared.domain.enums.SettlementScopeType;
import com.spvermicelli.tripledger.shared.domain.enums.SettlementStatus;
import com.spvermicelli.tripledger.shared.domain.enums.SettlementStrategyType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 结算应用服务。
 * V1 当前采用“个人视角结算”：
 * 1. 只为当前登录成员计算自己与其他成员之间的净额关系；
 * 2. 不暴露与当前成员无关的成员间转账链路；
 * 3. 结算批次只是当下快照，不冻结账单。
 */
@Service
public class SettlementApplicationService {

    private final BillingAccessSupportService billingAccessSupportService;
    private final StatisticsApplicationService statisticsApplicationService;
    private final SettlementBatchRepository settlementBatchRepository;
    private final SettlementTransferRepository settlementTransferRepository;
    private final BookMemberRepository bookMemberRepository;
    private final BookUserDirectory bookUserDirectory;
    private final ObjectMapper objectMapper;

    public SettlementApplicationService(
        BillingAccessSupportService billingAccessSupportService,
        StatisticsApplicationService statisticsApplicationService,
        SettlementBatchRepository settlementBatchRepository,
        SettlementTransferRepository settlementTransferRepository,
        BookMemberRepository bookMemberRepository,
        BookUserDirectory bookUserDirectory,
        ObjectMapper objectMapper
    ) {
        this.billingAccessSupportService = billingAccessSupportService;
        this.statisticsApplicationService = statisticsApplicationService;
        this.settlementBatchRepository = settlementBatchRepository;
        this.settlementTransferRepository = settlementTransferRepository;
        this.bookMemberRepository = bookMemberRepository;
        this.bookUserDirectory = bookUserDirectory;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SettlementBatchResult createSettlement(CreateSettlementCommand command) {
        if (command.getCurrentUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前登录信息不存在");
        }
        SettlementStrategyType strategyType = parseStrategyType(command.getStrategyType());
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(command.getCurrentUserId(), command.getBookId());

        SettlementBatch batch = settlementBatchRepository.save(SettlementBatch.builder()
            .bookId(command.getBookId())
            .initiatorMemberId(context.getCurrentMember().getId())
            .strategyType(strategyType)
            .scopeType(SettlementScopeType.PERSONAL_VIEW)
            .status(SettlementStatus.SUCCESS)
            .snapshotTime(LocalDateTime.now())
            .build());

        List<SettlementTransfer> transfers = buildTransfers(command.getBookId(), context.getCurrentMember(), batch.getId());
        settlementTransferRepository.saveAll(transfers);
        List<SettlementTransfer> savedTransfers = settlementTransferRepository.findBySettlementBatchId(batch.getId());

        return toBatchResult(batch, savedTransfers, loadMemberMap(command.getBookId()));
    }

    @Transactional(readOnly = true)
    public SettlementBatchResult getSettlementDetail(Long currentUserId, Long bookId, Long settlementBatchId) {
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(currentUserId, bookId);
        SettlementBatch batch = settlementBatchRepository.findById(settlementBatchId)
            .filter(item -> Objects.equals(item.getBookId(), bookId))
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "结算批次不存在"));
        if (!Objects.equals(batch.getInitiatorMemberId(), context.getCurrentMember().getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无权查看该结算批次");
        }
        return toBatchResult(batch, settlementTransferRepository.findBySettlementBatchId(batch.getId()), loadMemberMap(bookId));
    }

    @Transactional(readOnly = true)
    public List<SettlementBatchResult> getSettlementHistory(Long currentUserId, Long bookId) {
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(currentUserId, bookId);
        Map<Long, BookMember> memberMap = loadMemberMap(bookId);
        return settlementBatchRepository.findByBookIdAndInitiatorMemberId(bookId, context.getCurrentMember().getId()).stream()
            .map(batch -> toBatchResult(batch, settlementTransferRepository.findBySettlementBatchId(batch.getId()), memberMap))
            .toList();
    }

    private List<SettlementTransfer> buildTransfers(Long bookId, BookMember currentMember, Long settlementBatchId) {
        List<MemberRelationResult> memberRelations = statisticsApplicationService.getMemberRelations(currentMember.getUserId(), bookId);
        LocalDateTime now = LocalDateTime.now();
        return memberRelations.stream()
            .filter(item -> "MEMBER".equals(item.getTargetParticipantType()))
            .filter(item -> item.getTargetMemberId() != null)
            .filter(item -> item.getNetAmountCent() != null && item.getNetAmountCent() > 0)
            .map(item -> {
                if ("TARGET_OWES_ME".equals(item.getNetDirection())) {
                    return SettlementTransfer.builder()
                        .settlementBatchId(settlementBatchId)
                        .fromMemberId(item.getTargetMemberId())
                        .toMemberId(currentMember.getId())
                        .transferAmountCent(item.getNetAmountCent())
                        .relatedSummaryJson(toSummaryJson("TARGET_OWES_ME", item))
                        .createdAt(now)
                        .build();
                }
                return SettlementTransfer.builder()
                    .settlementBatchId(settlementBatchId)
                    .fromMemberId(currentMember.getId())
                    .toMemberId(item.getTargetMemberId())
                    .transferAmountCent(item.getNetAmountCent())
                    .relatedSummaryJson(toSummaryJson("I_OWE_TARGET", item))
                    .createdAt(now)
                    .build();
            })
            .toList();
    }

    private SettlementBatchResult toBatchResult(
        SettlementBatch batch,
        List<SettlementTransfer> transfers,
        Map<Long, BookMember> memberMap
    ) {
        Map<Long, BookUserSummary> userSummaryMap = loadUserSummaryMap(memberMap.values());
        return SettlementBatchResult.builder()
            .settlementBatchId(batch.getId())
            .bookId(batch.getBookId())
            .initiatorMemberId(batch.getInitiatorMemberId())
            .strategyType(batch.getStrategyType().getCode())
            .scopeType(batch.getScopeType().getCode())
            .status(batch.getStatus().getCode())
            .snapshotTime(batch.getSnapshotTime())
            .createdAt(batch.getCreatedAt())
            .transferList(transfers.stream()
                .map(transfer -> toTransferResult(transfer, memberMap, userSummaryMap))
                .toList())
            .build();
    }

    private SettlementTransferResult toTransferResult(
        SettlementTransfer transfer,
        Map<Long, BookMember> memberMap,
        Map<Long, BookUserSummary> userSummaryMap
    ) {
        BookMember fromMember = memberMap.get(transfer.getFromMemberId());
        BookMember toMember = memberMap.get(transfer.getToMemberId());
        BookUserSummary fromSummary = fromMember == null ? null : userSummaryMap.get(fromMember.getUserId());
        BookUserSummary toSummary = toMember == null ? null : userSummaryMap.get(toMember.getUserId());
        return SettlementTransferResult.builder()
            .transferId(transfer.getId())
            .fromMemberId(transfer.getFromMemberId())
            .fromMemberName(fromSummary == null ? null : fromSummary.getNickname())
            .toMemberId(transfer.getToMemberId())
            .toMemberName(toSummary == null ? null : toSummary.getNickname())
            .transferAmountCent(transfer.getTransferAmountCent())
            .relatedSummaryJson(transfer.getRelatedSummaryJson())
            .build();
    }

    private Map<Long, BookMember> loadMemberMap(Long bookId) {
        return bookMemberRepository.findActiveByBookId(bookId).stream()
            .collect(Collectors.toMap(BookMember::getId, Function.identity()));
    }

    private Map<Long, BookUserSummary> loadUserSummaryMap(Iterable<BookMember> members) {
        java.util.List<BookMember> list = new java.util.ArrayList<>();
        members.forEach(list::add);
        return bookUserDirectory.getByUserIds(list.stream()
            .map(BookMember::getUserId)
            .distinct()
            .toList());
    }

    private String toSummaryJson(String direction, MemberRelationResult relation) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "direction", direction,
                "targetMemberId", relation.getTargetMemberId(),
                "netAmountCent", relation.getNetAmountCent()
            ));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "结算摘要生成失败");
        }
    }

    private SettlementStrategyType parseStrategyType(String rawType) {
        if (rawType == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "strategyType 不能为空");
        }
        try {
            return SettlementStrategyType.valueOf(rawType.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "strategyType 仅支持 INTUITIVE_FIRST 或 MIN_TRANSFER_COUNT");
        }
    }
}
