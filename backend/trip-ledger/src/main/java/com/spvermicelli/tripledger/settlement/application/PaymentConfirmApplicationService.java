package com.spvermicelli.tripledger.settlement.application;

import com.spvermicelli.tripledger.billing.application.support.BillingAccessSupportService;
import com.spvermicelli.tripledger.billing.application.support.BillingBookMemberContext;
import com.spvermicelli.tripledger.billing.application.support.BillingProjectionService;
import com.spvermicelli.tripledger.billing.application.support.PaymentAllocationDraft;
import com.spvermicelli.tripledger.ledger.domain.book.model.BookMember;
import com.spvermicelli.tripledger.ledger.domain.book.repository.BookMemberRepository;
import com.spvermicelli.tripledger.ledger.domain.book.service.BookUserDirectory;
import com.spvermicelli.tripledger.ledger.domain.book.valueobject.BookUserSummary;
import com.spvermicelli.tripledger.settlement.application.command.ConfirmPaymentCommand;
import com.spvermicelli.tripledger.settlement.application.command.CreateDirectReceiveCommand;
import com.spvermicelli.tripledger.settlement.application.command.CreatePaymentConfirmCommand;
import com.spvermicelli.tripledger.settlement.application.command.RejectPaymentCommand;
import com.spvermicelli.tripledger.settlement.application.result.PaymentConfirmAllocationResult;
import com.spvermicelli.tripledger.settlement.application.result.PaymentConfirmResult;
import com.spvermicelli.tripledger.settlement.domain.model.PaymentConfirmAllocation;
import com.spvermicelli.tripledger.settlement.domain.model.PaymentConfirmRecord;
import com.spvermicelli.tripledger.settlement.domain.model.SettlementBatch;
import com.spvermicelli.tripledger.settlement.domain.model.SettlementTransfer;
import com.spvermicelli.tripledger.settlement.domain.repository.SettlementBatchRepository;
import com.spvermicelli.tripledger.settlement.domain.repository.PaymentConfirmAllocationRepository;
import com.spvermicelli.tripledger.settlement.domain.repository.PaymentConfirmRecordRepository;
import com.spvermicelli.tripledger.settlement.domain.repository.SettlementTransferRepository;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.spvermicelli.tripledger.shared.application.operation.OperationLogService;
import com.spvermicelli.tripledger.shared.domain.enums.OperationTargetType;
import com.spvermicelli.tripledger.shared.domain.enums.OperationType;
import com.spvermicelli.tripledger.shared.domain.enums.PaymentConfirmStatus;
import com.spvermicelli.tripledger.shared.domain.enums.PaymentSourceType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 支付确认应用服务。
 * 该服务处理“成员之间已付款/已收款”的确认链路，并把支付金额精确分配回具体账单。
 */
@Service
public class PaymentConfirmApplicationService {

    private final BillingAccessSupportService billingAccessSupportService;
    private final BillingProjectionService billingProjectionService;
    private final PaymentConfirmRecordRepository paymentConfirmRecordRepository;
    private final PaymentConfirmAllocationRepository paymentConfirmAllocationRepository;
    private final SettlementBatchRepository settlementBatchRepository;
    private final SettlementTransferRepository settlementTransferRepository;
    private final BookMemberRepository bookMemberRepository;
    private final BookUserDirectory bookUserDirectory;
    private final OperationLogService operationLogService;

    public PaymentConfirmApplicationService(
        BillingAccessSupportService billingAccessSupportService,
        BillingProjectionService billingProjectionService,
        PaymentConfirmRecordRepository paymentConfirmRecordRepository,
        PaymentConfirmAllocationRepository paymentConfirmAllocationRepository,
        SettlementBatchRepository settlementBatchRepository,
        SettlementTransferRepository settlementTransferRepository,
        BookMemberRepository bookMemberRepository,
        BookUserDirectory bookUserDirectory,
        OperationLogService operationLogService
    ) {
        this.billingAccessSupportService = billingAccessSupportService;
        this.billingProjectionService = billingProjectionService;
        this.paymentConfirmRecordRepository = paymentConfirmRecordRepository;
        this.paymentConfirmAllocationRepository = paymentConfirmAllocationRepository;
        this.settlementBatchRepository = settlementBatchRepository;
        this.settlementTransferRepository = settlementTransferRepository;
        this.bookMemberRepository = bookMemberRepository;
        this.bookUserDirectory = bookUserDirectory;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public PaymentConfirmResult createPaymentConfirm(CreatePaymentConfirmCommand command) {
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(command.getCurrentUserId(), command.getBookId());
        validateBaseCreateCommand(command.getPaymentAmountCent(), command.getToMemberId());
        BookMember fromMember = context.getCurrentMember();
        BookMember toMember = requireActiveMember(command.getBookId(), command.getToMemberId(), "收款成员不存在");
        ensureNotSelf(fromMember.getId(), toMember.getId());

        PaymentSourceType sourceType = parseSourceType(command.getSourceType());
        validateTransferSource(command.getBookId(), sourceType, command.getSourceRefId(), fromMember.getId(), toMember.getId());

        List<PaymentAllocationDraft> allocationDrafts = billingProjectionService.buildPaymentAllocationDrafts(
            command.getBookId(),
            fromMember.getId(),
            toMember.getId(),
            command.getPaymentAmountCent()
        );
        long allocatedAmount = allocationDrafts.stream().mapToLong(PaymentAllocationDraft::getAllocatedAmountCent).sum();
        if (!Objects.equals(allocatedAmount, command.getPaymentAmountCent())) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前待支付金额不足，无法匹配本次付款金额");
        }

        PaymentConfirmRecord savedRecord = paymentConfirmRecordRepository.save(PaymentConfirmRecord.builder()
            .bookId(command.getBookId())
            .fromMemberId(fromMember.getId())
            .toMemberId(toMember.getId())
            .paymentAmountCent(command.getPaymentAmountCent())
            .sourceType(sourceType)
            .sourceRefId(command.getSourceRefId())
            .confirmStatus(PaymentConfirmStatus.PENDING_CONFIRM)
            .initiatedByMemberId(fromMember.getId())
            .confirmedByMemberId(null)
            .initiatedAt(LocalDateTime.now())
            .confirmedAt(null)
            .autoConfirmAt(LocalDateTime.now().plusHours(48))
            .remark(normalizeText(command.getRemark()))
            .build());
        saveAllocations(savedRecord.getId(), allocationDrafts);
        operationLogService.log(
            command.getBookId(),
            fromMember.getUserId(),
            fromMember.getId(),
            OperationType.INITIATE_PAYMENT_CONFIRM,
            OperationTargetType.PAYMENT_CONFIRM_RECORD,
            savedRecord.getId(),
            null,
            "status=PENDING_CONFIRM,amount=" + savedRecord.getPaymentAmountCent()
        );

        return toResult(savedRecord);
    }

    @Transactional(readOnly = true)
    public List<PaymentConfirmResult> getPendingReceiveList(Long currentUserId, Long bookId) {
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(currentUserId, bookId);
        return paymentConfirmRecordRepository.findByBookIdAndToMemberIdAndStatus(
                bookId,
                context.getCurrentMember().getId(),
                PaymentConfirmStatus.PENDING_CONFIRM
            ).stream()
            .map(this::toResult)
            .toList();
    }

    @Transactional
    public PaymentConfirmResult confirmPayment(ConfirmPaymentCommand command) {
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(command.getCurrentUserId(), command.getBookId());
        PaymentConfirmRecord record = paymentConfirmRecordRepository.findById(command.getPaymentConfirmId())
            .filter(item -> Objects.equals(item.getBookId(), command.getBookId()))
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "支付确认记录不存在"));
        if (!Objects.equals(record.getToMemberId(), context.getCurrentMember().getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有收款方可以确认该付款记录");
        }
        if (!record.isPendingConfirm()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "该付款记录当前不是待确认状态");
        }

        PaymentConfirmRecord savedRecord = paymentConfirmRecordRepository.save(record.confirm(context.getCurrentMember().getId(), LocalDateTime.now()));
        operationLogService.log(
            command.getBookId(),
            context.getCurrentMember().getUserId(),
            context.getCurrentMember().getId(),
            OperationType.CONFIRM_PAYMENT,
            OperationTargetType.PAYMENT_CONFIRM_RECORD,
            savedRecord.getId(),
            "status=" + record.getConfirmStatus().getCode(),
            "status=" + savedRecord.getConfirmStatus().getCode()
        );
        return toResult(savedRecord);
    }

    @Transactional
    public PaymentConfirmResult createDirectReceive(CreateDirectReceiveCommand command) {
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(command.getCurrentUserId(), command.getBookId());
        validateBaseCreateCommand(command.getPaymentAmountCent(), command.getFromMemberId());

        BookMember toMember = context.getCurrentMember();
        BookMember fromMember = requireActiveMember(command.getBookId(), command.getFromMemberId(), "付款成员不存在");
        ensureNotSelf(fromMember.getId(), toMember.getId());

        PaymentSourceType sourceType = parseSourceType(command.getSourceType());
        validateTransferSource(command.getBookId(), sourceType, command.getSourceRefId(), fromMember.getId(), toMember.getId());

        List<PaymentAllocationDraft> allocationDrafts = billingProjectionService.buildPaymentAllocationDrafts(
            command.getBookId(),
            fromMember.getId(),
            toMember.getId(),
            command.getPaymentAmountCent()
        );
        long allocatedAmount = allocationDrafts.stream().mapToLong(PaymentAllocationDraft::getAllocatedAmountCent).sum();
        if (!Objects.equals(allocatedAmount, command.getPaymentAmountCent())) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前待支付金额不足，无法匹配本次已收款金额");
        }

        LocalDateTime now = LocalDateTime.now();
        PaymentConfirmRecord savedRecord = paymentConfirmRecordRepository.save(PaymentConfirmRecord.builder()
            .bookId(command.getBookId())
            .fromMemberId(fromMember.getId())
            .toMemberId(toMember.getId())
            .paymentAmountCent(command.getPaymentAmountCent())
            .sourceType(sourceType)
            .sourceRefId(command.getSourceRefId())
            .confirmStatus(PaymentConfirmStatus.CONFIRMED)
            .initiatedByMemberId(toMember.getId())
            .confirmedByMemberId(toMember.getId())
            .initiatedAt(now)
            .confirmedAt(now)
            .autoConfirmAt(null)
            .remark(normalizeText(command.getRemark()))
            .build());
        saveAllocations(savedRecord.getId(), allocationDrafts);
        operationLogService.log(
            command.getBookId(),
            toMember.getUserId(),
            toMember.getId(),
            OperationType.DIRECT_RECEIVE_PAYMENT,
            OperationTargetType.PAYMENT_CONFIRM_RECORD,
            savedRecord.getId(),
            null,
            "status=CONFIRMED,amount=" + savedRecord.getPaymentAmountCent()
        );

        return toResult(savedRecord);
    }

    @Transactional(readOnly = true)
    public List<PaymentConfirmResult> getPendingPayList(Long currentUserId, Long bookId) {
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(currentUserId, bookId);
        return paymentConfirmRecordRepository.findByBookIdAndFromMemberIdAndStatus(
                bookId,
                context.getCurrentMember().getId(),
                PaymentConfirmStatus.PENDING_CONFIRM
            ).stream()
            .map(this::toResult)
            .toList();
    }

    @Transactional
    public PaymentConfirmResult rejectPayment(RejectPaymentCommand command) {
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(command.getCurrentUserId(), command.getBookId());
        PaymentConfirmRecord record = paymentConfirmRecordRepository.findById(command.getPaymentConfirmId())
            .filter(item -> Objects.equals(item.getBookId(), command.getBookId()))
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "支付确认记录不存在"));
        if (!Objects.equals(record.getToMemberId(), context.getCurrentMember().getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有收款方可以拒绝该付款请求");
        }
        if (!record.isPendingConfirm()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "该付款记录当前不是待确认状态");
        }

        PaymentConfirmRecord savedRecord = paymentConfirmRecordRepository.save(record.reject(context.getCurrentMember().getId(), LocalDateTime.now()));
        operationLogService.log(
            command.getBookId(),
            context.getCurrentMember().getUserId(),
            context.getCurrentMember().getId(),
            OperationType.REJECT_PAYMENT,
            OperationTargetType.PAYMENT_CONFIRM_RECORD,
            savedRecord.getId(),
            "status=" + record.getConfirmStatus().getCode(),
            "status=" + savedRecord.getConfirmStatus().getCode() + ",remark=" + normalizeText(command.getRejectRemark())
        );
        return toResult(savedRecord);
    }

    @Transactional(readOnly = true)
    public List<PaymentConfirmResult> getRelationHistory(Long currentUserId, Long bookId, Long targetMemberId, String direction) {
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(currentUserId, bookId);
        Long currentMemberId = context.getCurrentMember().getId();
        String safeDirection = StringUtils.hasText(direction) ? direction.trim().toUpperCase() : "ALL";

        if (targetMemberId != null) {
            requireActiveMember(bookId, targetMemberId, "目标成员不存在");
            if (Objects.equals(targetMemberId, currentMemberId)) {
                throw new BusinessException(ErrorCode.INVALID_PARAM, "目标成员不能是自己");
            }
        }

        return paymentConfirmRecordRepository.findByBookId(bookId).stream()
            .filter(record -> belongsToDirection(record, currentMemberId, targetMemberId, safeDirection))
            .map(this::toResult)
            .toList();
    }

    private PaymentConfirmResult toResult(PaymentConfirmRecord record) {
        Map<Long, BookMember> memberMap = java.util.stream.Stream.of(record.getFromMemberId(), record.getToMemberId())
            .map(bookMemberRepository::findById)
            .flatMap(java.util.Optional::stream)
            .collect(Collectors.toMap(BookMember::getId, Function.identity()));
        Map<Long, BookUserSummary> userSummaryMap = bookUserDirectory.getByUserIds(memberMap.values().stream()
            .map(BookMember::getUserId)
            .toList());

        BookMember fromMember = memberMap.get(record.getFromMemberId());
        BookMember toMember = memberMap.get(record.getToMemberId());
        BookUserSummary fromSummary = fromMember == null ? null : userSummaryMap.get(fromMember.getUserId());
        BookUserSummary toSummary = toMember == null ? null : userSummaryMap.get(toMember.getUserId());

        return PaymentConfirmResult.builder()
            .paymentConfirmId(record.getId())
            .bookId(record.getBookId())
            .fromMemberId(record.getFromMemberId())
            .fromMemberName(fromSummary == null ? null : fromSummary.getNickname())
            .toMemberId(record.getToMemberId())
            .toMemberName(toSummary == null ? null : toSummary.getNickname())
            .paymentAmountCent(record.getPaymentAmountCent())
            .sourceType(record.getSourceType().getCode())
            .sourceRefId(record.getSourceRefId())
            .confirmStatus(record.getConfirmStatus().getCode())
            .initiatedByMemberId(record.getInitiatedByMemberId())
            .confirmedByMemberId(record.getConfirmedByMemberId())
            .initiatedAt(record.getInitiatedAt())
            .confirmedAt(record.getConfirmedAt())
            .autoConfirmAt(record.getAutoConfirmAt())
            .remark(record.getRemark())
            .allocationList(paymentConfirmAllocationRepository.findByPaymentConfirmId(record.getId()).stream()
                .map(allocation -> PaymentConfirmAllocationResult.builder()
                    .billId(allocation.getBillId())
                    .allocatedAmountCent(allocation.getAllocatedAmountCent())
                    .build())
                .toList())
            .build();
    }

    private void validateBaseCreateCommand(Long paymentAmountCent, Long targetMemberId) {
        if (targetMemberId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "目标成员不能为空");
        }
        if (paymentAmountCent == null || paymentAmountCent <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "paymentAmountCent 必须大于 0");
        }
    }

    private BookMember requireActiveMember(Long bookId, Long memberId, String errorMessage) {
        return bookMemberRepository.findById(memberId)
            .filter(BookMember::isActive)
            .filter(member -> Objects.equals(member.getBookId(), bookId))
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, errorMessage));
    }

    private void ensureNotSelf(Long fromMemberId, Long toMemberId) {
        if (Objects.equals(fromMemberId, toMemberId)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "付款方和收款方不能是同一成员");
        }
    }

    private PaymentSourceType parseSourceType(String rawType) {
        String source = StringUtils.hasText(rawType) ? rawType.trim().toUpperCase() : PaymentSourceType.MANUAL.getCode();
        try {
            return PaymentSourceType.valueOf(source);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "sourceType 仅支持 SETTLEMENT_TRANSFER 或 MANUAL");
        }
    }

    private void validateTransferSource(
        Long bookId,
        PaymentSourceType sourceType,
        Long sourceRefId,
        Long fromMemberId,
        Long toMemberId
    ) {
        if (sourceType != PaymentSourceType.SETTLEMENT_TRANSFER) {
            return;
        }
        if (sourceRefId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "来源为结算建议时，sourceRefId 不能为空");
        }
        SettlementTransfer transfer = settlementTransferRepository.findById(sourceRefId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "结算建议不存在"));
        SettlementBatch settlementBatch = settlementBatchRepository.findById(transfer.getSettlementBatchId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "结算建议不存在"));
        if (!Objects.equals(settlementBatch.getBookId(), bookId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "结算建议不属于当前账本");
        }
        if (!Objects.equals(transfer.getFromMemberId(), fromMemberId) || !Objects.equals(transfer.getToMemberId(), toMemberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "结算建议与当前付款关系不匹配");
        }
    }

    private void saveAllocations(Long paymentConfirmId, List<PaymentAllocationDraft> drafts) {
        paymentConfirmAllocationRepository.saveAll(paymentConfirmId, drafts.stream()
            .map(draft -> PaymentConfirmAllocation.builder()
                .paymentConfirmId(paymentConfirmId)
                .billId(draft.getBillId())
                .allocatedAmountCent(draft.getAllocatedAmountCent())
                .build())
            .toList());
    }

    private String normalizeText(String rawText) {
        return StringUtils.hasText(rawText) ? rawText.trim() : null;
    }

    private boolean belongsToDirection(PaymentConfirmRecord record, Long currentMemberId, Long targetMemberId, String direction) {
        if ("RECEIVABLE".equals(direction)) {
            if (!Objects.equals(record.getToMemberId(), currentMemberId)) {
                return false;
            }
            return targetMemberId == null || Objects.equals(record.getFromMemberId(), targetMemberId);
        }
        if ("PAYABLE".equals(direction)) {
            if (!Objects.equals(record.getFromMemberId(), currentMemberId)) {
                return false;
            }
            return targetMemberId == null || Objects.equals(record.getToMemberId(), targetMemberId);
        }
        if (targetMemberId == null) {
            return Objects.equals(record.getFromMemberId(), currentMemberId) || Objects.equals(record.getToMemberId(), currentMemberId);
        }
        return (Objects.equals(record.getFromMemberId(), currentMemberId) && Objects.equals(record.getToMemberId(), targetMemberId))
            || (Objects.equals(record.getToMemberId(), currentMemberId) && Objects.equals(record.getFromMemberId(), targetMemberId));
    }
}
