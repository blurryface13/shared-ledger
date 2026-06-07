package com.spvermicelli.tripledger.settlement.application;

import com.spvermicelli.tripledger.billing.application.support.BillingAccessSupportService;
import com.spvermicelli.tripledger.billing.application.support.BillingBookMemberContext;
import com.spvermicelli.tripledger.billing.application.support.BillingProjectionService;
import com.spvermicelli.tripledger.billing.application.support.TempRecoveryAllocationDraft;
import com.spvermicelli.tripledger.ledger.domain.book.model.BookMember;
import com.spvermicelli.tripledger.ledger.domain.book.repository.BookMemberRepository;
import com.spvermicelli.tripledger.ledger.domain.tempParticipant.model.TempParticipant;
import com.spvermicelli.tripledger.ledger.domain.tempParticipant.repository.TempParticipantRepository;
import com.spvermicelli.tripledger.settlement.application.command.CreateTempRecoveryCommand;
import com.spvermicelli.tripledger.settlement.application.result.TempRecoveryAllocationResult;
import com.spvermicelli.tripledger.settlement.application.result.TempRecoveryResult;
import com.spvermicelli.tripledger.settlement.domain.model.TempRecoveryAllocation;
import com.spvermicelli.tripledger.settlement.domain.model.TempRecoveryRecord;
import com.spvermicelli.tripledger.settlement.domain.repository.TempRecoveryAllocationRepository;
import com.spvermicelli.tripledger.settlement.domain.repository.TempRecoveryRecordRepository;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.spvermicelli.tripledger.shared.domain.enums.RecoveryConfirmStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 临时成员回补应用服务。
 * 该模块只允许挂靠正式成员确认回补，并把回补金额精确分配到对应账单。
 */
@Service
public class TempRecoveryApplicationService {

    private final BillingAccessSupportService billingAccessSupportService;
    private final BillingProjectionService billingProjectionService;
    private final TempRecoveryRecordRepository tempRecoveryRecordRepository;
    private final TempRecoveryAllocationRepository tempRecoveryAllocationRepository;
    private final TempParticipantRepository tempParticipantRepository;
    private final BookMemberRepository bookMemberRepository;

    public TempRecoveryApplicationService(
        BillingAccessSupportService billingAccessSupportService,
        BillingProjectionService billingProjectionService,
        TempRecoveryRecordRepository tempRecoveryRecordRepository,
        TempRecoveryAllocationRepository tempRecoveryAllocationRepository,
        TempParticipantRepository tempParticipantRepository,
        BookMemberRepository bookMemberRepository
    ) {
        this.billingAccessSupportService = billingAccessSupportService;
        this.billingProjectionService = billingProjectionService;
        this.tempRecoveryRecordRepository = tempRecoveryRecordRepository;
        this.tempRecoveryAllocationRepository = tempRecoveryAllocationRepository;
        this.tempParticipantRepository = tempParticipantRepository;
        this.bookMemberRepository = bookMemberRepository;
    }

    @Transactional
    public TempRecoveryResult createTempRecovery(CreateTempRecoveryCommand command) {
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(command.getCurrentUserId(), command.getBookId());
        if (command.getTempParticipantId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "tempParticipantId 不能为空");
        }
        if (command.getRecoveryAmountCent() == null || command.getRecoveryAmountCent() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "recoveryAmountCent 必须大于 0");
        }

        TempParticipant tempParticipant = tempParticipantRepository.findById(command.getTempParticipantId())
            .filter(item -> Objects.equals(item.getBookId(), command.getBookId()))
            .filter(TempParticipant::isActive)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "临时成员不存在"));
        if (!Objects.equals(tempParticipant.getAttachedMemberId(), context.getCurrentMember().getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有当前挂靠正式成员可以确认该临时成员回补");
        }

        List<TempRecoveryAllocationDraft> drafts = billingProjectionService.buildTempRecoveryAllocationDrafts(
            command.getBookId(),
            tempParticipant.getId(),
            context.getCurrentMember().getId(),
            command.getRecoveryAmountCent()
        );
        long allocatedAmount = drafts.stream().mapToLong(TempRecoveryAllocationDraft::getAllocatedAmountCent).sum();
        if (!Objects.equals(allocatedAmount, command.getRecoveryAmountCent())) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前临时成员待回补金额不足，无法记录本次回补");
        }

        LocalDateTime now = LocalDateTime.now();
        TempRecoveryRecord savedRecord = tempRecoveryRecordRepository.save(TempRecoveryRecord.builder()
            .bookId(command.getBookId())
            .tempParticipantId(tempParticipant.getId())
            .attachedMemberId(context.getCurrentMember().getId())
            .recoveryAmountCent(command.getRecoveryAmountCent())
            .confirmStatus(RecoveryConfirmStatus.CONFIRMED)
            .confirmedByMemberId(context.getCurrentMember().getId())
            .createdAt(now)
            .confirmedAt(now)
            .remark(normalizeText(command.getRemark()))
            .build());
        tempRecoveryAllocationRepository.saveAll(savedRecord.getId(), drafts.stream()
            .map(draft -> TempRecoveryAllocation.builder()
                .recoveryRecordId(savedRecord.getId())
                .billId(draft.getBillId())
                .allocatedAmountCent(draft.getAllocatedAmountCent())
                .build())
            .toList());
        return toResult(savedRecord);
    }

    @Transactional(readOnly = true)
    public List<TempRecoveryResult> getTempRecoveryList(Long currentUserId, Long bookId) {
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(currentUserId, bookId);
        return tempRecoveryRecordRepository.findByBookIdAndAttachedMemberId(bookId, context.getCurrentMember().getId()).stream()
            .map(this::toResult)
            .toList();
    }

    private TempRecoveryResult toResult(TempRecoveryRecord record) {
        TempParticipant tempParticipant = tempParticipantRepository.findById(record.getTempParticipantId()).orElse(null);
        BookMember attachedMember = bookMemberRepository.findById(record.getAttachedMemberId()).orElse(null);
        return TempRecoveryResult.builder()
            .recoveryRecordId(record.getId())
            .bookId(record.getBookId())
            .tempParticipantId(record.getTempParticipantId())
            .tempParticipantNickname(tempParticipant == null ? null : tempParticipant.getNickname())
            .attachedMemberId(record.getAttachedMemberId())
            .recoveryAmountCent(record.getRecoveryAmountCent())
            .confirmStatus(record.getConfirmStatus().getCode())
            .confirmedByMemberId(record.getConfirmedByMemberId())
            .createdAt(record.getCreatedAt())
            .confirmedAt(record.getConfirmedAt())
            .remark(record.getRemark())
            .allocationList(tempRecoveryAllocationRepository.findByRecoveryRecordId(record.getId()).stream()
                .map(item -> TempRecoveryAllocationResult.builder()
                    .billId(item.getBillId())
                    .allocatedAmountCent(item.getAllocatedAmountCent())
                    .build())
                .toList())
            .build();
    }

    private String normalizeText(String rawText) {
        return StringUtils.hasText(rawText) ? rawText.trim() : null;
    }
}
