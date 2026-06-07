package com.spvermicelli.tripledger.billing.application.statistics;

import com.spvermicelli.tripledger.billing.application.support.BillingAccessSupportService;
import com.spvermicelli.tripledger.billing.application.support.BillingBookMemberContext;
import com.spvermicelli.tripledger.billing.application.support.BillingProjection;
import com.spvermicelli.tripledger.billing.application.support.BillingProjectionService;
import com.spvermicelli.tripledger.billing.application.support.SharedDebtLine;
import com.spvermicelli.tripledger.billing.application.support.TempRecoveryAllocationView;
import com.spvermicelli.tripledger.billing.application.support.TempRecoveryLine;
import com.spvermicelli.tripledger.billing.domain.bill.model.Bill;
import com.spvermicelli.tripledger.billing.domain.bill.model.BillShareItem;
import com.spvermicelli.tripledger.billing.application.statistics.result.AttachedTempDetailResult;
import com.spvermicelli.tripledger.billing.application.statistics.result.CategoryConsumptionResult;
import com.spvermicelli.tripledger.billing.application.statistics.result.MemberRelationBillDetailResult;
import com.spvermicelli.tripledger.billing.application.statistics.result.MemberRelationResult;
import com.spvermicelli.tripledger.billing.application.statistics.result.StatisticsOverviewResult;
import com.spvermicelli.tripledger.ledger.domain.book.model.BookMember;
import com.spvermicelli.tripledger.ledger.domain.book.repository.BookMemberRepository;
import com.spvermicelli.tripledger.ledger.domain.book.service.BookUserDirectory;
import com.spvermicelli.tripledger.ledger.domain.book.valueobject.BookUserSummary;
import com.spvermicelli.tripledger.ledger.domain.category.model.BookCategory;
import com.spvermicelli.tripledger.ledger.domain.category.repository.BookCategoryRepository;
import com.spvermicelli.tripledger.ledger.domain.tempParticipant.model.TempParticipant;
import com.spvermicelli.tripledger.ledger.domain.tempParticipant.repository.TempParticipantRepository;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.spvermicelli.tripledger.shared.domain.enums.TempParticipantType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 统计应用服务。
 * 统计全部建立在“当前用户可进入该账本”的前提之上，真正的金额口径则基于账单投影统一计算。
 */
@Service
public class StatisticsApplicationService {

    private final BillingAccessSupportService billingAccessSupportService;
    private final BillingProjectionService billingProjectionService;
    private final BookCategoryRepository bookCategoryRepository;
    private final BookMemberRepository bookMemberRepository;
    private final BookUserDirectory bookUserDirectory;
    private final TempParticipantRepository tempParticipantRepository;

    public StatisticsApplicationService(
        BillingAccessSupportService billingAccessSupportService,
        BillingProjectionService billingProjectionService,
        BookCategoryRepository bookCategoryRepository,
        BookMemberRepository bookMemberRepository,
        BookUserDirectory bookUserDirectory,
        TempParticipantRepository tempParticipantRepository
    ) {
        this.billingAccessSupportService = billingAccessSupportService;
        this.billingProjectionService = billingProjectionService;
        this.bookCategoryRepository = bookCategoryRepository;
        this.bookMemberRepository = bookMemberRepository;
        this.bookUserDirectory = bookUserDirectory;
        this.tempParticipantRepository = tempParticipantRepository;
    }

    @Transactional(readOnly = true)
    public StatisticsOverviewResult getOverview(Long currentUserId, Long bookId) {
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(currentUserId, bookId);
        BillingProjection projection = billingProjectionService.loadProjection(bookId);
        Long currentMemberId = context.getCurrentMember().getId();

        long personalExpenseTotal = projection.getBillMap().values().stream()
            .filter(Bill::isPersonalExpense)
            .filter(bill -> Objects.equals(bill.getPayerMemberId(), currentMemberId))
            .mapToLong(Bill::getBillAmountCent)
            .sum();
        long personalIncomeTotal = projection.getBillMap().values().stream()
            .filter(Bill::isPersonalIncome)
            .filter(bill -> Objects.equals(bill.getRecorderMemberId(), currentMemberId))
            .mapToLong(Bill::getBillAmountCent)
            .sum();
        long personalCarryPayTotal = projection.getBillMap().values().stream()
            .filter(Bill::isPersonalCarry)
            .filter(bill -> Objects.equals(bill.getPayerMemberId(), currentMemberId))
            .mapToLong(Bill::getBillAmountCent)
            .sum();
        long sharedPayTotal = projection.getBillMap().values().stream()
            .filter(Bill::isSharedExpense)
            .filter(bill -> Objects.equals(bill.getPayerMemberId(), currentMemberId))
            .mapToLong(Bill::getBillAmountCent)
            .sum();

        long sharedOwnLiabilityTotal = projection.getShareItemsByBillId().entrySet().stream()
            .flatMap(entry -> entry.getValue().stream())
            .filter(item -> item.getParticipantType().name().equals("MEMBER"))
            .filter(item -> Objects.equals(item.getParticipantRefId(), currentMemberId))
            .mapToLong(BillShareItem::getShareAmountCent)
            .sum();

        PaymentSplitTotals paymentSplitTotals = splitSharedPaymentTotals(projection, currentMemberId);
        long sharedReceivableTotal = projection.getSharedDebtLines().stream()
            .filter(line -> Objects.equals(line.getToMemberId(), currentMemberId))
            .mapToLong(line -> outstandingSharedDebt(line, projection))
            .sum();
        long personalCarryRecoveredTotal = projection.getTempRecoveryLines().stream()
            .filter(line -> Objects.equals(line.getReceiverMemberId(), currentMemberId))
            .filter(line -> projection.getBillMap().get(line.getBillId()).isPersonalCarry())
            .mapToLong(line -> confirmedRecovery(line, projection))
            .sum();
        long personalCarryUnrecoveredTotal = projection.getTempRecoveryLines().stream()
            .filter(line -> Objects.equals(line.getReceiverMemberId(), currentMemberId))
            .filter(line -> projection.getBillMap().get(line.getBillId()).isPersonalCarry())
            .mapToLong(line -> Math.max(line.getAmountCent() - confirmedRecovery(line, projection), 0L))
            .sum();
        long attachedTempRecoveredTotal = projection.getTempRecoveryLines().stream()
            .filter(line -> Objects.equals(line.getReceiverMemberId(), currentMemberId))
            .filter(line -> projection.getBillMap().get(line.getBillId()).isSharedExpense())
            .mapToLong(line -> confirmedRecovery(line, projection))
            .sum();
        long attachedTempUnrecoveredTotal = projection.getTempRecoveryLines().stream()
            .filter(line -> Objects.equals(line.getReceiverMemberId(), currentMemberId))
            .filter(line -> projection.getBillMap().get(line.getBillId()).isSharedExpense())
            .mapToLong(line -> Math.max(line.getAmountCent() - confirmedRecovery(line, projection), 0L))
            .sum();

        long payFlowAmount = personalExpenseTotal + personalCarryPayTotal + sharedPayTotal + paymentSplitTotals.ownConfirmedPaid();
        long accruedConsumptionAmount = personalExpenseTotal + sharedOwnLiabilityTotal;
        long receivableAmount = sharedReceivableTotal + personalCarryUnrecoveredTotal + attachedTempUnrecoveredTotal;
        long pendingContributionAmount = paymentSplitTotals.ownOutstanding() + paymentSplitTotals.attachedOutstanding();
        long recoveredFlowAmount = personalCarryRecoveredTotal
            + confirmedSharedReceivedTotal(projection, currentMemberId)
            + attachedTempRecoveredTotal;
        long budgetAmount = context.getCurrentMember().getBudgetAmountCent() == null
            ? 0L
            : Math.max(context.getCurrentMember().getBudgetAmountCent(), 0L);
        double budgetUsagePercent = budgetAmount <= 0
            ? 0D
            : (accruedConsumptionAmount * 100D / budgetAmount);

        return StatisticsOverviewResult.builder()
            .payFlowAmountCent(payFlowAmount)
            .accruedConsumptionAmountCent(accruedConsumptionAmount)
            .receivableAmountCent(receivableAmount)
            .pendingContributionAmountCent(pendingContributionAmount)
            .personalIncomeAmountCent(personalIncomeTotal)
            .recoveredFlowAmountCent(recoveredFlowAmount)
            .settledConsumptionAmountCent(accruedConsumptionAmount)
            .currentMemberBudgetAmountCent(budgetAmount)
            .budgetUsagePercent(Math.round(budgetUsagePercent * 100D) / 100D)
            .build();
    }

    @Transactional
    public Long updateCurrentMemberBudget(Long currentUserId, Long bookId, Long budgetAmountCent) {
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(currentUserId, bookId);
        long normalized = budgetAmountCent == null ? 0L : budgetAmountCent;
        if (normalized < 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "预算金额不能小于 0");
        }
        if (normalized > 1_000_000_000_00L) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "预算金额超出允许范围");
        }
        bookMemberRepository.save(context.getCurrentMember().updateBudgetAmountCent(normalized));
        return normalized;
    }

    @Transactional(readOnly = true)
    public List<CategoryConsumptionResult> getCategoryConsumption(Long currentUserId, Long bookId) {
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(currentUserId, bookId);
        BillingProjection projection = billingProjectionService.loadProjection(bookId);
        Map<Long, BookCategory> categoryMap = bookCategoryRepository.findVisibleForBook(bookId, currentUserId).stream()
            .collect(Collectors.toMap(BookCategory::getId, Function.identity(), (left, right) -> left));

        Map<Long, Long> amountByCategory = new HashMap<>();
        projection.getBillMap().values().stream()
            .filter(Bill::isPersonalExpense)
            .filter(bill -> Objects.equals(bill.getPayerMemberId(), context.getCurrentMember().getId()))
            .forEach(bill -> amountByCategory.merge(bill.getCategoryId(), bill.getBillAmountCent(), Long::sum));

        projection.getShareItemsByBillId().forEach((billId, items) -> {
            Bill bill = projection.getBillMap().get(billId);
            if (bill == null || !bill.isSharedExpense()) {
                return;
            }
            items.stream()
                .filter(item -> item.getParticipantType().name().equals("MEMBER"))
                .filter(item -> Objects.equals(item.getParticipantRefId(), context.getCurrentMember().getId()))
                .forEach(item -> amountByCategory.merge(bill.getCategoryId(), item.getShareAmountCent(), Long::sum));
        });

        return amountByCategory.entrySet().stream()
            .map(entry -> {
                BookCategory category = categoryMap.get(entry.getKey());
                return CategoryConsumptionResult.builder()
                    .categoryId(entry.getKey())
                    .categoryName(category == null ? null : category.getName())
                    .categoryIcon(category == null ? null : category.getIcon())
                    .categoryType(category == null ? null : category.getCategoryType().getCode())
                    .consumptionAmountCent(entry.getValue())
                    .build();
            })
            .sorted(Comparator.comparing(CategoryConsumptionResult::getConsumptionAmountCent).reversed())
            .toList();
    }

    @Transactional(readOnly = true)
    public List<MemberRelationResult> getMemberRelations(Long currentUserId, Long bookId) {
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(currentUserId, bookId);
        BillingProjection projection = billingProjectionService.loadProjection(bookId);
        Long currentMemberId = context.getCurrentMember().getId();

        Map<Long, BookMember> memberMap = bookMemberRepository.findActiveByBookId(bookId).stream()
            .collect(Collectors.toMap(BookMember::getId, Function.identity()));
        Map<Long, BookUserSummary> userSummaryMap = loadUserSummaryMap(memberMap.values());
        Map<Long, TempParticipant> tempMap = tempParticipantRepository.findByBookId(bookId).stream()
            .collect(Collectors.toMap(TempParticipant::getId, Function.identity(), (left, right) -> left));

        List<MemberRelationResult> memberRelations = memberMap.values().stream()
            .filter(member -> !Objects.equals(member.getId(), currentMemberId))
            .map(member -> toMemberRelationResult(currentMemberId, member, projection, userSummaryMap))
            .collect(Collectors.toCollection(ArrayList::new));

        tempMap.values().stream()
            .filter(tempParticipant -> Objects.equals(tempParticipant.getAttachedMemberId(), currentMemberId))
            .map(tempParticipant -> toTempRelationResult(tempParticipant, projection))
            .forEach(memberRelations::add);

        return memberRelations.stream()
            .sorted(Comparator
                .comparing(MemberRelationResult::getTargetParticipantType)
                .thenComparing(MemberRelationResult::getTargetParticipantId))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<MemberRelationBillDetailResult> getMemberRelationBillDetails(
        Long currentUserId,
        Long bookId,
        String direction,
        String targetType,
        Long targetId
    ) {
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(currentUserId, bookId);
        BillingProjection projection = billingProjectionService.loadProjection(bookId);
        Long currentMemberId = context.getCurrentMember().getId();
        String normalizedDirection = (direction == null ? "" : direction).trim().toUpperCase();
        String normalizedTargetType = (targetType == null ? "" : targetType).trim().toUpperCase();
        if (!List.of("RECEIVABLE", "PAYABLE").contains(normalizedDirection)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "direction 仅支持 RECEIVABLE / PAYABLE");
        }
        if (!List.of("MEMBER", "TEMP_PARTICIPANT").contains(normalizedTargetType)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "targetType 仅支持 MEMBER / TEMP_PARTICIPANT");
        }
        if (targetId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "targetId 不能为空");
        }

        Map<Long, BookCategory> categoryMap = bookCategoryRepository.findVisibleForBook(bookId, currentUserId).stream()
            .collect(Collectors.toMap(BookCategory::getId, Function.identity(), (left, right) -> left));
        List<MemberRelationBillDetailResult> rows = new ArrayList<>();
        if ("TEMP_PARTICIPANT".equals(normalizedTargetType)) {
            if (!"RECEIVABLE".equals(normalizedDirection)) {
                return List.of();
            }
            projection.getTempRecoveryLines().stream()
                .filter(line -> Objects.equals(line.getReceiverMemberId(), currentMemberId))
                .filter(line -> Objects.equals(line.getTempParticipantId(), targetId))
                .forEach(line -> rows.add(toTempRecoveryDetail(line, projection, categoryMap)));
        } else if ("RECEIVABLE".equals(normalizedDirection)) {
            projection.getSharedDebtLines().stream()
                .filter(line -> Objects.equals(line.getToMemberId(), currentMemberId))
                .filter(line -> Objects.equals(line.getFromMemberId(), targetId))
                .collect(Collectors.groupingBy(SharedDebtLine::getBillId))
                .values()
                .forEach(lines -> rows.add(toSharedDebtDetail(lines, projection, categoryMap, true)));
        } else {
            projection.getSharedDebtLines().stream()
                .filter(line -> Objects.equals(line.getFromMemberId(), currentMemberId))
                .filter(line -> Objects.equals(line.getToMemberId(), targetId))
                .collect(Collectors.groupingBy(SharedDebtLine::getBillId))
                .values()
                .forEach(lines -> rows.add(toSharedDebtDetail(lines, projection, categoryMap, false)));
        }
        return rows.stream()
            .sorted(Comparator
                .comparing(MemberRelationBillDetailResult::isSettled)
                .thenComparing(MemberRelationBillDetailResult::getBillTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MemberRelationBillDetailResult::getBillId))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<AttachedTempDetailResult> getAttachedTempDetails(Long currentUserId, Long bookId) {
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(currentUserId, bookId);
        BillingProjection projection = billingProjectionService.loadProjection(bookId);
        Long currentMemberId = context.getCurrentMember().getId();

        return tempParticipantRepository.findByBookId(bookId).stream()
            .filter(tempParticipant -> Objects.equals(tempParticipant.getAttachedMemberId(), currentMemberId))
            .map(tempParticipant -> toAttachedTempDetailResult(tempParticipant, projection))
            .sorted(Comparator.comparing(AttachedTempDetailResult::getTotalReceivableAmountCent).reversed())
            .toList();
    }

    private MemberRelationResult toMemberRelationResult(
        Long currentMemberId,
        BookMember targetMember,
        BillingProjection projection,
        Map<Long, BookUserSummary> userSummaryMap
    ) {
        long totalIOwe = projection.getSharedDebtLines().stream()
            .filter(line -> Objects.equals(line.getFromMemberId(), currentMemberId))
            .filter(line -> Objects.equals(line.getToMemberId(), targetMember.getId()))
            .mapToLong(SharedDebtLine::getAmountCent)
            .sum();
        long iOweTargetTemp = projection.getSharedDebtLines().stream()
            .filter(line -> Objects.equals(line.getFromMemberId(), currentMemberId))
            .filter(line -> Objects.equals(line.getToMemberId(), targetMember.getId()))
            .filter(line -> line.getSourceTempParticipantId() != null)
            .mapToLong(SharedDebtLine::getAmountCent)
            .sum();
        long paidOut = projection.getPaymentAllocationViews().stream()
            .filter(view -> Objects.equals(view.getFromMemberId(), currentMemberId))
            .filter(view -> Objects.equals(view.getToMemberId(), targetMember.getId()))
            .filter(view -> isConfirmedLike(view.getConfirmStatus().name()))
            .mapToLong(view -> view.getAllocatedAmountCent())
            .sum();
        long totalTargetOwesMe = projection.getSharedDebtLines().stream()
            .filter(line -> Objects.equals(line.getFromMemberId(), targetMember.getId()))
            .filter(line -> Objects.equals(line.getToMemberId(), currentMemberId))
            .mapToLong(SharedDebtLine::getAmountCent)
            .sum();
        long targetOwesMeTemp = projection.getSharedDebtLines().stream()
            .filter(line -> Objects.equals(line.getFromMemberId(), targetMember.getId()))
            .filter(line -> Objects.equals(line.getToMemberId(), currentMemberId))
            .filter(line -> line.getSourceTempParticipantId() != null)
            .mapToLong(SharedDebtLine::getAmountCent)
            .sum();
        long targetPaidMe = projection.getPaymentAllocationViews().stream()
            .filter(view -> Objects.equals(view.getFromMemberId(), targetMember.getId()))
            .filter(view -> Objects.equals(view.getToMemberId(), currentMemberId))
            .filter(view -> isConfirmedLike(view.getConfirmStatus().name()))
            .mapToLong(view -> view.getAllocatedAmountCent())
            .sum();

        long iStillNeedPay = Math.max(totalIOwe - paidOut, 0L);
        long targetStillNeedPay = Math.max(totalTargetOwesMe - targetPaidMe, 0L);

        String netDirection;
        long netAmount;
        if (targetStillNeedPay > iStillNeedPay) {
            netDirection = "TARGET_OWES_ME";
            netAmount = targetStillNeedPay - iStillNeedPay;
        } else if (iStillNeedPay > targetStillNeedPay) {
            netDirection = "I_OWE_TARGET";
            netAmount = iStillNeedPay - targetStillNeedPay;
        } else {
            netDirection = "BALANCED";
            netAmount = 0L;
        }

        BookUserSummary summary = userSummaryMap.get(targetMember.getUserId());
        return MemberRelationResult.builder()
            .targetParticipantType("MEMBER")
            .targetParticipantId(targetMember.getId())
            .targetMemberId(targetMember.getId())
            .targetMemberName(summary == null ? null : summary.getNickname())
            .targetMemberAvatarUrl(summary == null ? null : summary.getAvatarUrl())
            .iOweTargetAmountCent(totalIOwe)
            .iPaidTargetAmountCent(paidOut)
            .iStillNeedPayTargetAmountCent(iStillNeedPay)
            .iOweTargetTempAmountCent(iOweTargetTemp)
            .targetOwesMeAmountCent(totalTargetOwesMe)
            .targetPaidMeAmountCent(targetPaidMe)
            .targetStillNeedPayMeAmountCent(targetStillNeedPay)
            .targetOwesMeTempAmountCent(targetOwesMeTemp)
            .netDirection(netDirection)
            .netAmountCent(netAmount)
            .build();
    }

    private MemberRelationResult toTempRelationResult(TempParticipant tempParticipant, BillingProjection projection) {
        List<TempRecoveryLine> tempLines = projection.getTempRecoveryLines().stream()
            .filter(line -> Objects.equals(line.getTempParticipantId(), tempParticipant.getId()))
            .filter(line -> Objects.equals(line.getReceiverMemberId(), tempParticipant.getAttachedMemberId()))
            .toList();
        long totalReceivable = tempLines.stream().mapToLong(TempRecoveryLine::getAmountCent).sum();
        long recovered = tempLines.stream().mapToLong(line -> confirmedRecovery(line, projection)).sum();
        return MemberRelationResult.builder()
            .targetParticipantType("TEMP_PARTICIPANT")
            .targetParticipantId(tempParticipant.getId())
            .targetMemberId(null)
            .targetMemberName(tempParticipant.getNickname())
            .targetMemberAvatarUrl(null)
            .iOweTargetAmountCent(0L)
            .iPaidTargetAmountCent(0L)
            .iStillNeedPayTargetAmountCent(0L)
            .iOweTargetTempAmountCent(0L)
            .targetOwesMeAmountCent(totalReceivable)
            .targetPaidMeAmountCent(recovered)
            .targetStillNeedPayMeAmountCent(Math.max(totalReceivable - recovered, 0L))
            .targetOwesMeTempAmountCent(0L)
            .netDirection(totalReceivable > recovered ? "TARGET_OWES_ME" : "BALANCED")
            .netAmountCent(Math.max(totalReceivable - recovered, 0L))
            .build();
    }

    private MemberRelationBillDetailResult toSharedDebtDetail(
        List<SharedDebtLine> lines,
        BillingProjection projection,
        Map<Long, BookCategory> categoryMap,
        boolean receivable
    ) {
        SharedDebtLine line = lines.getFirst();
        Bill bill = projection.getBillMap().get(line.getBillId());
        BookCategory category = bill == null ? null : categoryMap.get(bill.getCategoryId());
        long paid = lines.stream()
            .mapToLong(item -> confirmedPaymentForDebt(item, projection))
            .sum();
        long amount = lines.stream()
            .mapToLong(item -> Math.max(item.getAmountCent(), 0L))
            .sum();
        return MemberRelationBillDetailResult.builder()
            .billId(line.getBillId())
            .billType(bill == null ? null : bill.getBillType().getCode())
            .title(bill == null ? null : bill.getTitle())
            .remark(bill == null ? null : bill.getRemark())
            .categoryId(bill == null ? null : bill.getCategoryId())
            .categoryName(category == null ? null : category.getName())
            .categoryIcon(category == null ? null : category.getIcon())
            .billTime(bill == null ? null : bill.getBillTime())
            .relationTag(receivable ? "应收" : "应付")
            .amountCent(amount)
            .paidAmountCent(paid)
            .unpaidAmountCent(Math.max(amount - paid, 0L))
            .settled(Math.max(amount - paid, 0L) <= 0)
            .tempIncluded(lines.stream().anyMatch(item -> item.getSourceTempParticipantId() != null))
            .build();
    }

    private MemberRelationBillDetailResult toTempRecoveryDetail(
        TempRecoveryLine line,
        BillingProjection projection,
        Map<Long, BookCategory> categoryMap
    ) {
        Bill bill = projection.getBillMap().get(line.getBillId());
        BookCategory category = bill == null ? null : categoryMap.get(bill.getCategoryId());
        long paid = confirmedRecovery(line, projection);
        long amount = Math.max(line.getAmountCent(), 0L);
        return MemberRelationBillDetailResult.builder()
            .billId(line.getBillId())
            .billType(bill == null ? null : bill.getBillType().getCode())
            .title(bill == null ? null : bill.getTitle())
            .remark(bill == null ? null : bill.getRemark())
            .categoryId(bill == null ? null : bill.getCategoryId())
            .categoryName(category == null ? null : category.getName())
            .categoryIcon(category == null ? null : category.getIcon())
            .billTime(bill == null ? null : bill.getBillTime())
            .relationTag("临时成员应收")
            .amountCent(amount)
            .paidAmountCent(paid)
            .unpaidAmountCent(Math.max(amount - paid, 0L))
            .settled(Math.max(amount - paid, 0L) <= 0)
            .tempIncluded(true)
            .build();
    }

    private AttachedTempDetailResult toAttachedTempDetailResult(TempParticipant tempParticipant, BillingProjection projection) {
        List<TempRecoveryLine> tempLines = projection.getTempRecoveryLines().stream()
            .filter(line -> Objects.equals(line.getTempParticipantId(), tempParticipant.getId()))
            .toList();
        long totalReceivable = tempLines.stream().mapToLong(TempRecoveryLine::getAmountCent).sum();
        long recovered = tempLines.stream().mapToLong(line -> confirmedRecovery(line, projection)).sum();
        long sharedExpenseReceivable = tempLines.stream()
            .filter(line -> projection.getBillMap().get(line.getBillId()).isSharedExpense())
            .mapToLong(TempRecoveryLine::getAmountCent)
            .sum();
        long personalCarryReceivable = tempLines.stream()
            .filter(line -> projection.getBillMap().get(line.getBillId()).isPersonalCarry())
            .mapToLong(TempRecoveryLine::getAmountCent)
            .sum();

        return AttachedTempDetailResult.builder()
            .tempParticipantId(tempParticipant.getId())
            .tempParticipantNickname(tempParticipant.getNickname())
            .tempParticipantType(TempParticipantType.safeCode(tempParticipant.getTempType()))
            .totalReceivableAmountCent(totalReceivable)
            .sharedExpenseReceivableAmountCent(sharedExpenseReceivable)
            .personalCarryReceivableAmountCent(personalCarryReceivable)
            .recoveredAmountCent(recovered)
            .unrecoveredAmountCent(Math.max(totalReceivable - recovered, 0L))
            .build();
    }

    private PaymentSplitTotals splitSharedPaymentTotals(BillingProjection projection, Long currentMemberId) {
        long ownConfirmedPaid = 0L;
        long attachedConfirmedPaid = 0L;
        long ownOutstanding = 0L;
        long attachedOutstanding = 0L;

        Map<Long, Long> confirmedPaidByBill = projection.getPaymentAllocationViews().stream()
            .filter(view -> Objects.equals(view.getFromMemberId(), currentMemberId))
            .filter(view -> isConfirmedLike(view.getConfirmStatus().name()))
            .collect(Collectors.groupingBy(
                view -> view.getBillId(),
                Collectors.summingLong(view -> view.getAllocatedAmountCent())
            ));

        Map<Long, List<SharedDebtLine>> debtByBill = projection.getSharedDebtLines().stream()
            .filter(line -> Objects.equals(line.getFromMemberId(), currentMemberId))
            .collect(Collectors.groupingBy(SharedDebtLine::getBillId));

        for (Map.Entry<Long, List<SharedDebtLine>> entry : debtByBill.entrySet()) {
            long ownDebt = entry.getValue().stream()
                .filter(line -> line.getSourceTempParticipantId() == null)
                .mapToLong(SharedDebtLine::getAmountCent)
                .sum();
            long tempDebt = entry.getValue().stream()
                .filter(line -> line.getSourceTempParticipantId() != null)
                .mapToLong(SharedDebtLine::getAmountCent)
                .sum();
            long confirmed = confirmedPaidByBill.getOrDefault(entry.getKey(), 0L);
            long ownPaid = Math.min(ownDebt, confirmed);
            long tempPaid = Math.min(tempDebt, Math.max(confirmed - ownPaid, 0L));

            ownConfirmedPaid += ownPaid;
            attachedConfirmedPaid += tempPaid;
            ownOutstanding += Math.max(ownDebt - ownPaid, 0L);
            attachedOutstanding += Math.max(tempDebt - tempPaid, 0L);
        }
        return new PaymentSplitTotals(ownConfirmedPaid, attachedConfirmedPaid, ownOutstanding, attachedOutstanding);
    }

    private long confirmedSharedReceivedTotal(BillingProjection projection, Long currentMemberId) {
        return projection.getPaymentAllocationViews().stream()
            .filter(view -> Objects.equals(view.getToMemberId(), currentMemberId))
            .filter(view -> isConfirmedLike(view.getConfirmStatus().name()))
            .mapToLong(view -> view.getAllocatedAmountCent())
            .sum();
    }

    private long outstandingSharedDebt(SharedDebtLine line, BillingProjection projection) {
        return Math.max(line.getAmountCent() - confirmedPaymentForDebt(line, projection), 0L);
    }

    private long confirmedPaymentForDebt(SharedDebtLine line, BillingProjection projection) {
        long confirmed = projection.getPaymentAllocationViews().stream()
            .filter(view -> Objects.equals(view.getBillId(), line.getBillId()))
            .filter(view -> Objects.equals(view.getFromMemberId(), line.getFromMemberId()))
            .filter(view -> Objects.equals(view.getToMemberId(), line.getToMemberId()))
            .filter(view -> isConfirmedLike(view.getConfirmStatus().name()))
            .mapToLong(view -> view.getAllocatedAmountCent())
            .sum();
        List<SharedDebtLine> relatedLines = projection.getSharedDebtLines().stream()
            .filter(item -> Objects.equals(item.getBillId(), line.getBillId()))
            .filter(item -> Objects.equals(item.getFromMemberId(), line.getFromMemberId()))
            .filter(item -> Objects.equals(item.getToMemberId(), line.getToMemberId()))
            .sorted(Comparator.comparing(item -> item.getSourceTempParticipantId() != null))
            .toList();
        long remaining = confirmed;
        for (SharedDebtLine item : relatedLines) {
            long paidForLine = Math.min(Math.max(remaining, 0L), Math.max(item.getAmountCent(), 0L));
            if (item == line) {
                return paidForLine;
            }
            remaining -= Math.max(item.getAmountCent(), 0L);
        }
        return 0L;
    }

    private long confirmedRecovery(TempRecoveryLine line, BillingProjection projection) {
        return projection.getTempRecoveryAllocationViews().stream()
            .filter(view -> Objects.equals(view.getBillId(), line.getBillId()))
            .filter(view -> Objects.equals(view.getTempParticipantId(), line.getTempParticipantId()))
            .filter(view -> Objects.equals(view.getAttachedMemberId(), line.getReceiverMemberId()))
            .mapToLong(TempRecoveryAllocationView::getAllocatedAmountCent)
            .sum();
    }

    private Map<Long, BookUserSummary> loadUserSummaryMap(Collection<BookMember> members) {
        return bookUserDirectory.getByUserIds(members.stream()
            .map(BookMember::getUserId)
            .distinct()
            .toList());
    }

    private boolean isConfirmedLike(String statusCode) {
        return "CONFIRMED".equals(statusCode) || "AUTO_CONFIRMED".equals(statusCode);
    }

    private record PaymentSplitTotals(
        long ownConfirmedPaid,
        long attachedConfirmedPaid,
        long ownOutstanding,
        long attachedOutstanding
    ) {
    }
}
