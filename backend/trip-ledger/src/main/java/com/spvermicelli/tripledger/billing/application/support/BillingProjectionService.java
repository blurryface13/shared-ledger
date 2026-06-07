package com.spvermicelli.tripledger.billing.application.support;

import com.spvermicelli.tripledger.billing.domain.bill.model.Bill;
import com.spvermicelli.tripledger.billing.domain.bill.model.BillShareItem;
import com.spvermicelli.tripledger.billing.domain.bill.repository.BillRepository;
import com.spvermicelli.tripledger.billing.domain.bill.repository.BillShareItemRepository;
import com.spvermicelli.tripledger.ledger.domain.tempParticipant.model.TempParticipant;
import com.spvermicelli.tripledger.ledger.domain.tempParticipant.repository.TempParticipantRepository;
import com.spvermicelli.tripledger.settlement.domain.model.PaymentConfirmAllocation;
import com.spvermicelli.tripledger.settlement.domain.model.PaymentConfirmRecord;
import com.spvermicelli.tripledger.settlement.domain.model.TempRecoveryAllocation;
import com.spvermicelli.tripledger.settlement.domain.model.TempRecoveryRecord;
import com.spvermicelli.tripledger.settlement.domain.repository.PaymentConfirmAllocationRepository;
import com.spvermicelli.tripledger.settlement.domain.repository.PaymentConfirmRecordRepository;
import com.spvermicelli.tripledger.settlement.domain.repository.TempRecoveryAllocationRepository;
import com.spvermicelli.tripledger.settlement.domain.repository.TempRecoveryRecordRepository;
import com.spvermicelli.tripledger.shared.domain.enums.ShareParticipantType;
import com.spvermicelli.tripledger.shared.domain.enums.TempParticipantType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 账单投影与债务视图服务。
 * 这里集中完成三类计算：
 * 1. 把账本内账单及分摊项投影成可复用的共享债务线、临时成员回补线；
 * 2. 结合支付确认/回补确认分配记录，得到单账单级别的已支付/已回补结果；
 * 3. 为支付确认、回补确认提供“自动分配到哪些账单”的基础算法。
 *
 * 该服务本身不做权限判断，只负责“在一个账本范围内，账务数据长什么样”。
 */
@Component
public class BillingProjectionService {

    private final BillRepository billRepository;
    private final BillShareItemRepository billShareItemRepository;
    private final TempParticipantRepository tempParticipantRepository;
    private final PaymentConfirmRecordRepository paymentConfirmRecordRepository;
    private final PaymentConfirmAllocationRepository paymentConfirmAllocationRepository;
    private final TempRecoveryRecordRepository tempRecoveryRecordRepository;
    private final TempRecoveryAllocationRepository tempRecoveryAllocationRepository;

    public BillingProjectionService(
        BillRepository billRepository,
        BillShareItemRepository billShareItemRepository,
        TempParticipantRepository tempParticipantRepository,
        PaymentConfirmRecordRepository paymentConfirmRecordRepository,
        PaymentConfirmAllocationRepository paymentConfirmAllocationRepository,
        TempRecoveryRecordRepository tempRecoveryRecordRepository,
        TempRecoveryAllocationRepository tempRecoveryAllocationRepository
    ) {
        this.billRepository = billRepository;
        this.billShareItemRepository = billShareItemRepository;
        this.tempParticipantRepository = tempParticipantRepository;
        this.paymentConfirmRecordRepository = paymentConfirmRecordRepository;
        this.paymentConfirmAllocationRepository = paymentConfirmAllocationRepository;
        this.tempRecoveryRecordRepository = tempRecoveryRecordRepository;
        this.tempRecoveryAllocationRepository = tempRecoveryAllocationRepository;
    }

    public BillingProjection loadProjection(Long bookId) {
        List<Bill> bills = billRepository.findActiveByBookId(bookId);
        Map<Long, Bill> billMap = bills.stream().collect(Collectors.toMap(Bill::getId, Function.identity()));
        List<Long> billIds = bills.stream().map(Bill::getId).toList();

        Map<Long, List<BillShareItem>> shareItemsByBillId = billShareItemRepository.findByBillIds(
                bills.stream()
                    .filter(Bill::isSharedExpense)
                    .map(Bill::getId)
                    .toList())
            .stream()
            .collect(Collectors.groupingBy(BillShareItem::getBillId));

        Map<Long, TempParticipant> tempParticipantMap = tempParticipantRepository.findByBookId(bookId).stream()
            .collect(Collectors.toMap(TempParticipant::getId, Function.identity()));

        List<SharedDebtLine> sharedDebtLines = buildSharedDebtLines(bills, shareItemsByBillId, tempParticipantMap);
        List<TempRecoveryLine> tempRecoveryLines = buildTempRecoveryLines(bills, shareItemsByBillId, tempParticipantMap);
        List<PaymentAllocationView> paymentAllocationViews = buildPaymentAllocationViews(bookId, billMap.keySet());
        List<TempRecoveryAllocationView> tempRecoveryAllocationViews = buildTempRecoveryAllocationViews(bookId, billMap.keySet());

        return BillingProjection.builder()
            .billMap(billMap)
            .shareItemsByBillId(shareItemsByBillId)
            .sharedDebtLines(sharedDebtLines)
            .tempRecoveryLines(tempRecoveryLines)
            .paymentAllocationViews(paymentAllocationViews)
            .tempRecoveryAllocationViews(tempRecoveryAllocationViews)
            .build();
    }

    /**
     * 构造成员间支付确认的自动分配草稿。
     * 分配策略目前采用“按账单发生时间升序，再按账单 id 升序”消化未结清债务，
     * 这样能保持结果稳定、可解释，且适合商业系统的追溯需求。
     */
    public List<PaymentAllocationDraft> buildPaymentAllocationDrafts(
        Long bookId,
        Long fromMemberId,
        Long toMemberId,
        Long amountCent
    ) {
        if (amountCent == null || amountCent <= 0) {
            return Collections.emptyList();
        }

        BillingProjection projection = loadProjection(bookId);
        Map<Long, Long> confirmedAmountByBillId = projection.getPaymentAllocationViews().stream()
            .filter(view -> Objects.equals(view.getFromMemberId(), fromMemberId))
            .filter(view -> Objects.equals(view.getToMemberId(), toMemberId))
            .filter(view -> view.getConfirmStatus() != null && (
                view.getConfirmStatus().name().equals("CONFIRMED")
                    || view.getConfirmStatus().name().equals("AUTO_CONFIRMED")
            ))
            .collect(Collectors.groupingBy(PaymentAllocationView::getBillId, Collectors.summingLong(PaymentAllocationView::getAllocatedAmountCent)));

        long remaining = amountCent;
        List<PaymentAllocationDraft> drafts = new ArrayList<>();
        List<SharedDebtLine> candidateDebts = projection.getSharedDebtLines().stream()
            .filter(line -> Objects.equals(line.getFromMemberId(), fromMemberId))
            .filter(line -> Objects.equals(line.getToMemberId(), toMemberId))
            .sorted(Comparator
                .comparing((SharedDebtLine line) -> projection.getBillMap().get(line.getBillId()).getBillTime())
                .thenComparing(SharedDebtLine::getBillId))
            .toList();

        for (SharedDebtLine line : candidateDebts) {
            long confirmed = confirmedAmountByBillId.getOrDefault(line.getBillId(), 0L);
            long outstanding = Math.max(line.getAmountCent() - confirmed, 0L);
            if (outstanding <= 0) {
                continue;
            }
            long allocate = Math.min(outstanding, remaining);
            if (allocate > 0) {
                drafts.add(PaymentAllocationDraft.builder()
                    .billId(line.getBillId())
                    .allocatedAmountCent(allocate)
                    .build());
                remaining -= allocate;
            }
            if (remaining <= 0) {
                break;
            }
        }
        return drafts;
    }

    public List<TempRecoveryAllocationDraft> buildTempRecoveryAllocationDrafts(
        Long bookId,
        Long tempParticipantId,
        Long attachedMemberId,
        Long amountCent
    ) {
        if (amountCent == null || amountCent <= 0) {
            return Collections.emptyList();
        }

        BillingProjection projection = loadProjection(bookId);
        Map<Long, Long> confirmedAmountByBillId = projection.getTempRecoveryAllocationViews().stream()
            .filter(view -> Objects.equals(view.getTempParticipantId(), tempParticipantId))
            .filter(view -> Objects.equals(view.getAttachedMemberId(), attachedMemberId))
            .collect(Collectors.groupingBy(
                TempRecoveryAllocationView::getBillId,
                Collectors.summingLong(TempRecoveryAllocationView::getAllocatedAmountCent)
            ));

        long remaining = amountCent;
        List<TempRecoveryAllocationDraft> drafts = new ArrayList<>();
        List<TempRecoveryLine> candidateLines = projection.getTempRecoveryLines().stream()
            .filter(line -> Objects.equals(line.getTempParticipantId(), tempParticipantId))
            .filter(line -> Objects.equals(line.getReceiverMemberId(), attachedMemberId))
            .sorted(Comparator
                .comparing((TempRecoveryLine line) -> projection.getBillMap().get(line.getBillId()).getBillTime())
                .thenComparing(TempRecoveryLine::getBillId))
            .toList();

        for (TempRecoveryLine line : candidateLines) {
            long confirmed = confirmedAmountByBillId.getOrDefault(line.getBillId(), 0L);
            long outstanding = Math.max(line.getAmountCent() - confirmed, 0L);
            if (outstanding <= 0) {
                continue;
            }
            long allocate = Math.min(outstanding, remaining);
            if (allocate > 0) {
                drafts.add(TempRecoveryAllocationDraft.builder()
                    .billId(line.getBillId())
                    .allocatedAmountCent(allocate)
                    .build());
                remaining -= allocate;
            }
            if (remaining <= 0) {
                break;
            }
        }
        return drafts;
    }

    private List<SharedDebtLine> buildSharedDebtLines(
        List<Bill> bills,
        Map<Long, List<BillShareItem>> shareItemsByBillId,
        Map<Long, TempParticipant> tempParticipantMap
    ) {
        List<SharedDebtLine> lines = new ArrayList<>();
        for (Bill bill : bills) {
            if (bill.isPersonalCarry() && bill.getTargetTempParticipantId() != null) {
                TempParticipant tempParticipant = tempParticipantMap.get(bill.getTargetTempParticipantId());
                if (tempParticipant == null || tempParticipant.getAttachedMemberId() == null) {
                    continue;
                }
                if (TempParticipantType.normalize(tempParticipant.getTempType()) == TempParticipantType.GLOBAL
                    && !Objects.equals(tempParticipant.getAttachedMemberId(), bill.getPayerMemberId())) {
                    lines.add(SharedDebtLine.builder()
                        .billId(bill.getId())
                        .fromMemberId(tempParticipant.getAttachedMemberId())
                        .toMemberId(bill.getPayerMemberId())
                        .sourceTempParticipantId(tempParticipant.getId())
                        .amountCent(bill.getBillAmountCent())
                        .build());
                }
                continue;
            }

            if (!bill.isSharedExpense()) {
                continue;
            }
            for (BillShareItem item : shareItemsByBillId.getOrDefault(bill.getId(), List.of())) {
                if (item.getParticipantType() == ShareParticipantType.MEMBER) {
                    if (!Objects.equals(item.getParticipantRefId(), bill.getPayerMemberId())) {
                        lines.add(SharedDebtLine.builder()
                            .billId(bill.getId())
                            .fromMemberId(item.getParticipantRefId())
                            .toMemberId(bill.getPayerMemberId())
                            .sourceTempParticipantId(null)
                            .amountCent(item.getShareAmountCent())
                            .build());
                    }
                    continue;
                }

                TempParticipant tempParticipant = tempParticipantMap.get(item.getParticipantRefId());
                if (tempParticipant == null) {
                    continue;
                }
                if (!Objects.equals(tempParticipant.getAttachedMemberId(), bill.getPayerMemberId())) {
                    lines.add(SharedDebtLine.builder()
                        .billId(bill.getId())
                        .fromMemberId(tempParticipant.getAttachedMemberId())
                        .toMemberId(bill.getPayerMemberId())
                        .sourceTempParticipantId(tempParticipant.getId())
                        .amountCent(item.getShareAmountCent())
                        .build());
                }
            }
        }
        return lines;
    }

    private List<TempRecoveryLine> buildTempRecoveryLines(
        List<Bill> bills,
        Map<Long, List<BillShareItem>> shareItemsByBillId,
        Map<Long, TempParticipant> tempParticipantMap
    ) {
        List<TempRecoveryLine> lines = new ArrayList<>();
        for (Bill bill : bills) {
            if (bill.isPersonalCarry() && bill.getTargetTempParticipantId() != null) {
                TempParticipant tempParticipant = tempParticipantMap.get(bill.getTargetTempParticipantId());
                if (tempParticipant != null && tempParticipant.getAttachedMemberId() != null) {
                    lines.add(TempRecoveryLine.builder()
                        .billId(bill.getId())
                        .tempParticipantId(tempParticipant.getId())
                        .receiverMemberId(tempParticipant.getAttachedMemberId())
                        .amountCent(bill.getBillAmountCent())
                        .build());
                }
                continue;
            }

            if (!bill.isSharedExpense()) {
                continue;
            }
            for (BillShareItem item : shareItemsByBillId.getOrDefault(bill.getId(), List.of())) {
                if (item.getParticipantType() != ShareParticipantType.TEMP_PARTICIPANT) {
                    continue;
                }
                TempParticipant tempParticipant = tempParticipantMap.get(item.getParticipantRefId());
                if (tempParticipant == null) {
                    continue;
                }
                lines.add(TempRecoveryLine.builder()
                    .billId(bill.getId())
                    .tempParticipantId(tempParticipant.getId())
                    .receiverMemberId(tempParticipant.getAttachedMemberId())
                    .amountCent(item.getShareAmountCent())
                    .build());
            }
        }
        return lines;
    }

    private List<PaymentAllocationView> buildPaymentAllocationViews(Long bookId, Collection<Long> billIds) {
        if (billIds == null || billIds.isEmpty()) {
            return List.of();
        }
        List<PaymentConfirmRecord> records = paymentConfirmRecordRepository.findByBookId(bookId);
        Map<Long, PaymentConfirmRecord> recordMap = records.stream()
            .collect(Collectors.toMap(PaymentConfirmRecord::getId, Function.identity()));
        return paymentConfirmAllocationRepository.findByPaymentConfirmIds(recordMap.keySet()).stream()
            .filter(allocation -> billIds.contains(allocation.getBillId()))
            .map(allocation -> toPaymentAllocationView(allocation, recordMap))
            .filter(Objects::nonNull)
            .toList();
    }

    private List<TempRecoveryAllocationView> buildTempRecoveryAllocationViews(Long bookId, Collection<Long> billIds) {
        if (billIds == null || billIds.isEmpty()) {
            return List.of();
        }
        List<TempRecoveryRecord> records = tempRecoveryRecordRepository.findByBookId(bookId);
        Map<Long, TempRecoveryRecord> recordMap = records.stream()
            .collect(Collectors.toMap(TempRecoveryRecord::getId, Function.identity()));
        return tempRecoveryAllocationRepository.findByRecoveryRecordIds(recordMap.keySet()).stream()
            .filter(allocation -> billIds.contains(allocation.getBillId()))
            .map(allocation -> toTempRecoveryAllocationView(allocation, recordMap))
            .filter(Objects::nonNull)
            .toList();
    }

    private PaymentAllocationView toPaymentAllocationView(
        PaymentConfirmAllocation allocation,
        Map<Long, PaymentConfirmRecord> recordMap
    ) {
        PaymentConfirmRecord record = recordMap.get(allocation.getPaymentConfirmId());
        if (record == null) {
            return null;
        }
        return PaymentAllocationView.builder()
            .paymentConfirmId(record.getId())
            .billId(allocation.getBillId())
            .fromMemberId(record.getFromMemberId())
            .toMemberId(record.getToMemberId())
            .allocatedAmountCent(allocation.getAllocatedAmountCent())
            .confirmStatus(record.getConfirmStatus())
            .build();
    }

    private TempRecoveryAllocationView toTempRecoveryAllocationView(
        TempRecoveryAllocation allocation,
        Map<Long, TempRecoveryRecord> recordMap
    ) {
        TempRecoveryRecord record = recordMap.get(allocation.getRecoveryRecordId());
        if (record == null) {
            return null;
        }
        return TempRecoveryAllocationView.builder()
            .recoveryRecordId(record.getId())
            .billId(allocation.getBillId())
            .tempParticipantId(record.getTempParticipantId())
            .attachedMemberId(record.getAttachedMemberId())
            .allocatedAmountCent(allocation.getAllocatedAmountCent())
            .build();
    }
}
