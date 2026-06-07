package com.spvermicelli.tripledger.billing.application.bill;

import com.spvermicelli.tripledger.billing.application.bill.command.BillShareItemCommand;
import com.spvermicelli.tripledger.billing.application.bill.command.CreatePersonalBillCommand;
import com.spvermicelli.tripledger.billing.application.bill.command.CreateSharedExpenseBillCommand;
import com.spvermicelli.tripledger.billing.application.bill.command.DeleteBillCommand;
import com.spvermicelli.tripledger.billing.application.bill.command.UpdateBillCommand;
import com.spvermicelli.tripledger.billing.application.bill.result.BillDetailResult;
import com.spvermicelli.tripledger.billing.application.bill.result.BillListItemResult;
import com.spvermicelli.tripledger.billing.application.bill.result.BillOperationResult;
import com.spvermicelli.tripledger.billing.application.support.BillingAccessSupportService;
import com.spvermicelli.tripledger.billing.application.support.BillingBookMemberContext;
import com.spvermicelli.tripledger.billing.application.support.BillingProjection;
import com.spvermicelli.tripledger.billing.application.support.BillingProjectionService;
import com.spvermicelli.tripledger.billing.application.support.SharedDebtLine;
import com.spvermicelli.tripledger.billing.application.support.TempRecoveryAllocationView;
import com.spvermicelli.tripledger.billing.application.support.VisibleBillAggregate;
import com.spvermicelli.tripledger.billing.domain.bill.model.BillAttachment;
import com.spvermicelli.tripledger.billing.domain.bill.model.Bill;
import com.spvermicelli.tripledger.billing.domain.bill.model.BillShareItem;
import com.spvermicelli.tripledger.billing.domain.bill.repository.BillAttachmentRepository;
import com.spvermicelli.tripledger.billing.domain.bill.repository.BillRepository;
import com.spvermicelli.tripledger.billing.domain.bill.repository.BillShareItemRepository;
import com.spvermicelli.tripledger.billing.domain.request.model.BillChangeRequestAttachment;
import com.spvermicelli.tripledger.billing.domain.request.model.BillChangeRequestSnapshot;
import com.spvermicelli.tripledger.billing.domain.request.model.BillChangeRequestShareItem;
import com.spvermicelli.tripledger.billing.domain.request.model.BillChangeRequest;
import com.spvermicelli.tripledger.billing.domain.request.repository.BillChangeRequestAttachmentRepository;
import com.spvermicelli.tripledger.billing.domain.request.repository.BillChangeRequestRepository;
import com.spvermicelli.tripledger.billing.domain.request.repository.BillChangeRequestShareItemRepository;
import com.spvermicelli.tripledger.billing.domain.request.repository.BillChangeRequestSnapshotRepository;
import com.spvermicelli.tripledger.ledger.domain.book.model.Book;
import com.spvermicelli.tripledger.ledger.domain.book.model.BookMember;
import com.spvermicelli.tripledger.ledger.domain.book.repository.BookMemberRepository;
import com.spvermicelli.tripledger.ledger.domain.book.repository.BookRepository;
import com.spvermicelli.tripledger.ledger.domain.book.service.BookUserDirectory;
import com.spvermicelli.tripledger.ledger.domain.book.valueobject.BookUserSummary;
import com.spvermicelli.tripledger.ledger.domain.category.model.BookCategory;
import com.spvermicelli.tripledger.ledger.domain.category.repository.BookCategoryRepository;
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
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.spvermicelli.tripledger.shared.common.response.PageResponse;
import com.spvermicelli.tripledger.shared.domain.enums.BillStatus;
import com.spvermicelli.tripledger.shared.domain.enums.BillChangeFlowStatus;
import com.spvermicelli.tripledger.shared.domain.enums.BillType;
import com.spvermicelli.tripledger.shared.domain.enums.BookType;
import com.spvermicelli.tripledger.shared.domain.enums.CategoryType;
import com.spvermicelli.tripledger.shared.domain.enums.ChangeRequestStatus;
import com.spvermicelli.tripledger.shared.domain.enums.ChangeRequestType;
import com.spvermicelli.tripledger.shared.domain.enums.OperationTargetType;
import com.spvermicelli.tripledger.shared.domain.enums.OperationType;
import com.spvermicelli.tripledger.shared.domain.enums.PaymentConfirmStatus;
import com.spvermicelli.tripledger.shared.domain.enums.PaymentSourceType;
import com.spvermicelli.tripledger.shared.domain.enums.ShareMethod;
import com.spvermicelli.tripledger.shared.domain.enums.ShareParticipantType;
import com.spvermicelli.tripledger.shared.domain.enums.TempParticipantType;
import com.spvermicelli.tripledger.shared.domain.enums.RecoveryConfirmStatus;
import com.spvermicelli.tripledger.shared.application.operation.OperationLogService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 账单应用服务。
 * 该服务承担账单模块的核心用例编排职责：
 * 1. 创建、查询、修改、删除账单；
 * 2. 将账本成员关系、分类可见性、临时成员可见性等权限统一收回后端校验；
 * 3. 结合账单投影服务，生成前端真正可用的列表与详情数据。
 *
 * 说明：
 * 为保证统计、结算、支付确认、临时成员回补的历史链路稳定，
 * 当前对“已有支付/回补分配记录”的账单采用从严策略，不允许直接修改或删除。
 */
@Service
public class BillApplicationService {

    private final BookRepository bookRepository;
    private final BookMemberRepository bookMemberRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final TempParticipantRepository tempParticipantRepository;
    private final BillAttachmentRepository billAttachmentRepository;
    private final BillRepository billRepository;
    private final BillShareItemRepository billShareItemRepository;
    private final BookUserDirectory bookUserDirectory;
    private final BillingAccessSupportService billingAccessSupportService;
    private final BillingProjectionService billingProjectionService;
    private final PaymentConfirmRecordRepository paymentConfirmRecordRepository;
    private final PaymentConfirmAllocationRepository paymentConfirmAllocationRepository;
    private final TempRecoveryRecordRepository tempRecoveryRecordRepository;
    private final TempRecoveryAllocationRepository tempRecoveryAllocationRepository;
    private final BillChangeRequestRepository billChangeRequestRepository;
    private final BillChangeRequestSnapshotRepository billChangeRequestSnapshotRepository;
    private final BillChangeRequestShareItemRepository billChangeRequestShareItemRepository;
    private final BillChangeRequestAttachmentRepository billChangeRequestAttachmentRepository;
    private final OperationLogService operationLogService;

    public BillApplicationService(
        BookRepository bookRepository,
        BookMemberRepository bookMemberRepository,
        BookCategoryRepository bookCategoryRepository,
        TempParticipantRepository tempParticipantRepository,
        BillAttachmentRepository billAttachmentRepository,
        BillRepository billRepository,
        BillShareItemRepository billShareItemRepository,
        BookUserDirectory bookUserDirectory,
        BillingAccessSupportService billingAccessSupportService,
        BillingProjectionService billingProjectionService,
        PaymentConfirmRecordRepository paymentConfirmRecordRepository,
        PaymentConfirmAllocationRepository paymentConfirmAllocationRepository,
        TempRecoveryRecordRepository tempRecoveryRecordRepository,
        TempRecoveryAllocationRepository tempRecoveryAllocationRepository,
        BillChangeRequestRepository billChangeRequestRepository,
        BillChangeRequestSnapshotRepository billChangeRequestSnapshotRepository,
        BillChangeRequestShareItemRepository billChangeRequestShareItemRepository,
        BillChangeRequestAttachmentRepository billChangeRequestAttachmentRepository,
        OperationLogService operationLogService
    ) {
        this.bookRepository = bookRepository;
        this.bookMemberRepository = bookMemberRepository;
        this.bookCategoryRepository = bookCategoryRepository;
        this.tempParticipantRepository = tempParticipantRepository;
        this.billAttachmentRepository = billAttachmentRepository;
        this.billRepository = billRepository;
        this.billShareItemRepository = billShareItemRepository;
        this.bookUserDirectory = bookUserDirectory;
        this.billingAccessSupportService = billingAccessSupportService;
        this.billingProjectionService = billingProjectionService;
        this.paymentConfirmRecordRepository = paymentConfirmRecordRepository;
        this.paymentConfirmAllocationRepository = paymentConfirmAllocationRepository;
        this.tempRecoveryRecordRepository = tempRecoveryRecordRepository;
        this.tempRecoveryAllocationRepository = tempRecoveryAllocationRepository;
        this.billChangeRequestRepository = billChangeRequestRepository;
        this.billChangeRequestSnapshotRepository = billChangeRequestSnapshotRepository;
        this.billChangeRequestShareItemRepository = billChangeRequestShareItemRepository;
        this.billChangeRequestAttachmentRepository = billChangeRequestAttachmentRepository;
        this.operationLogService = operationLogService;
    }

    @Transactional(readOnly = true)
    public PageResponse<BillListItemResult> getVisibleBills(Long currentUserId, Long bookId, Integer pageNo, Integer pageSize) {
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(currentUserId, bookId);
        BillingProjection projection = billingProjectionService.loadProjection(bookId);
        Map<Long, BookCategory> categoryMap = loadCategoryMap(bookId, currentUserId);

        List<BillListItemResult> allItems = billingAccessSupportService.listVisibleBills(bookId, currentUserId).stream()
            .sorted(Comparator
                .comparing((VisibleBillAggregate aggregate) -> aggregate.getBill().getBillTime(), Comparator.nullsLast(LocalDateTime::compareTo))
                .reversed()
                .thenComparing(aggregate -> aggregate.getBill().getId(), Comparator.reverseOrder()))
            .map(aggregate -> toBillListItemResult(
                context.getCurrentMember(),
                aggregate.getBill(),
                aggregate.getShareItems(),
                projection,
                categoryMap
            ))
            .toList();

        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        int fromIndex = Math.min((safePageNo - 1) * safePageSize, allItems.size());
        int toIndex = Math.min(fromIndex + safePageSize, allItems.size());

        return PageResponse.<BillListItemResult>builder()
            .list(allItems.subList(fromIndex, toIndex))
            .pageNum(safePageNo)
            .pageSize(safePageSize)
            .total(allItems.size())
            .build();
    }

    @Transactional(readOnly = true)
    public BillDetailResult getBillDetail(Long currentUserId, Long bookId, Long billId) {
        VisibleBillAggregate visibleBill = billingAccessSupportService.requireVisibleBill(bookId, billId, currentUserId);
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(currentUserId, bookId);
        BillingProjection projection = billingProjectionService.loadProjection(bookId);
        Bill bill = visibleBill.getBill();

        Map<Long, BookCategory> categoryMap = loadCategoryMap(bookId, currentUserId);
        Map<Long, BookMember> memberMap = loadRelatedMemberMap(bookId, bill, visibleBill.getShareItems());
        Map<Long, BookUserSummary> userSummaryMap = loadUserSummaryMap(memberMap.values());
        Map<Long, TempParticipant> tempParticipantMap = loadTempParticipantMap(bookId);
        List<String> attachmentUrls = billAttachmentRepository.findByBillId(billId).stream()
            .map(BillAttachment::getFileUrl)
            .filter(StringUtils::hasText)
            .toList();

        BookCategory category = categoryMap.get(bill.getCategoryId());
        BookMember payerMember = memberMap.get(bill.getPayerMemberId());
        BookMember recorderMember = memberMap.get(bill.getRecorderMemberId());
        TempParticipant carryTarget = bill.getTargetTempParticipantId() == null
            ? null
            : tempParticipantMap.get(bill.getTargetTempParticipantId());

        long viewerReceivable = calculateViewerReceivable(context.getCurrentMember(), bill, projection);
        long viewerRecovered = calculateViewerRecovered(context.getCurrentMember(), bill, projection);
        long viewerOwnShare = calculateViewerOwnShare(context.getCurrentMember(), bill, visibleBill.getShareItems());
        long viewerOwnPaid = calculateViewerOwnPaid(context.getCurrentMember(), bill, projection);
        long viewerAttachedTempShare = calculateViewerAttachedTempShare(context.getCurrentMember(), bill, visibleBill.getShareItems());
        long viewerAttachedTempRecovered = calculateViewerAttachedTempRecovered(context.getCurrentMember(), bill, projection);
        long viewerReceivableTotal = viewerReceivable + (bill.isSharedExpense() ? viewerAttachedTempShare : 0L);
        long viewerRecoveredTotal = viewerRecovered + (bill.isSharedExpense() ? viewerAttachedTempRecovered : 0L);

        return BillDetailResult.builder()
            .billId(bill.getId())
            .billType(bill.getBillType().getCode())
            .title(bill.getTitle())
            .billAmountCent(bill.getBillAmountCent())
            .remark(bill.getRemark())
            .attachmentUrls(attachmentUrls)
            .changeFlowStatus(bill.getChangeFlowStatus() == null ? BillChangeFlowStatus.NONE.getCode() : bill.getChangeFlowStatus().getCode())
            .latestChangeRequestId(bill.getLatestChangeRequestId())
            .hasChangeHistory(bill.isHasChangeHistory())
            .categoryId(bill.getCategoryId())
            .categoryName(category == null ? null : category.getName())
            .categoryIcon(category == null ? null : category.getIcon())
            .billTime(bill.getBillTime())
            .payer(toMemberSummaryResult(payerMember, userSummaryMap))
            .recorder(toMemberSummaryResult(recorderMember, userSummaryMap))
            .carryTargetTempParticipant(toTempSummaryResult(carryTarget, memberMap, userSummaryMap))
            .directEditable(canDirectOperate(context.getCurrentMember(), bill, visibleBill.getShareItems()))
            .directDeletable(canDirectOperate(context.getCurrentMember(), bill, visibleBill.getShareItems()))
            .viewerReceivableAmountCent(viewerReceivableTotal)
            .viewerRecoveredAmountCent(viewerRecoveredTotal)
            .viewerUnrecoveredAmountCent(Math.max(viewerReceivableTotal - viewerRecoveredTotal, 0L))
            .viewerOwnShareAmountCent(viewerOwnShare)
            .viewerOwnPaidAmountCent(viewerOwnPaid)
            .viewerOwnUnpaidAmountCent(Math.max(viewerOwnShare - viewerOwnPaid, 0L))
            .viewerAttachedTempShareAmountCent(viewerAttachedTempShare)
            .viewerAttachedTempRecoveredAmountCent(viewerAttachedTempRecovered)
            .viewerAttachedTempUnrecoveredAmountCent(Math.max(viewerAttachedTempShare - viewerAttachedTempRecovered, 0L))
            .participants(toParticipantResults(bill, visibleBill.getShareItems(), projection, memberMap, userSummaryMap, tempParticipantMap))
            .createdAt(bill.getCreatedAt())
            .updatedAt(bill.getUpdatedAt())
            .build();
    }

    @Transactional
    public BillOperationResult settleSharedBillParticipant(
        Long currentUserId,
        Long bookId,
        Long billId,
        Long participantMemberId,
        String remark
    ) {
        if (billId == null || participantMemberId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "billId 和 participantMemberId 不能为空");
        }
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(currentUserId, bookId);
        VisibleBillAggregate visibleBill = billingAccessSupportService.requireVisibleBill(bookId, billId, currentUserId);
        Bill bill = visibleBill.getBill();
        if (!bill.isSharedExpense()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "仅共享账单支持成员结算");
        }
        if (!Objects.equals(context.getCurrentMember().getId(), bill.getPayerMemberId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅账单垫付人可执行该结算操作");
        }

        BillShareItem participantShare = visibleBill.getShareItems().stream()
            .filter(item -> item.getParticipantType() == ShareParticipantType.MEMBER)
            .filter(item -> Objects.equals(item.getParticipantRefId(), participantMemberId))
            .findFirst()
            .orElse(null);
        List<BillShareItem> attachedTempShares = visibleBill.getShareItems().stream()
            .filter(item -> item.getParticipantType() == ShareParticipantType.TEMP_PARTICIPANT)
            .filter(item -> Objects.equals(item.getAttachedMemberId(), participantMemberId))
            .toList();
        if (participantShare == null && attachedTempShares.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "该成员及其临时成员均未参与当前账单");
        }

        BillingProjection projection = billingProjectionService.loadProjection(bookId);
        long memberOutstandingAmount = 0L;
        if (participantShare != null) {
            long confirmedAmount = calculateConfirmedPaymentForBill(projection, billId, participantMemberId, bill.getPayerMemberId());
            memberOutstandingAmount = Math.max(participantShare.getShareAmountCent() - confirmedAmount, 0L);
        }
        List<TempSettleDraft> tempSettleDrafts = attachedTempShares.stream()
            .map(item -> {
                TempParticipant tempParticipant = tempParticipantRepository.findById(item.getParticipantRefId())
                    .filter(temp -> Objects.equals(temp.getBookId(), bookId))
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "临时成员不存在"));
                if (!Objects.equals(tempParticipant.getAttachedMemberId(), participantMemberId)) {
                    throw new BusinessException(ErrorCode.INVALID_STATUS, "临时成员挂靠关系已变更，请刷新后重试");
                }
                long confirmedRecovery = calculateConfirmedRecoveryForBill(
                    projection,
                    billId,
                    tempParticipant.getId(),
                    participantMemberId
                );
                long outstanding = Math.max(item.getShareAmountCent() - confirmedRecovery, 0L);
                return new TempSettleDraft(tempParticipant.getId(), outstanding);
            })
            .filter(draft -> draft.outstandingAmountCent() > 0)
            .toList();
        long tempOutstandingAmount = tempSettleDrafts.stream().mapToLong(TempSettleDraft::outstandingAmountCent).sum();
        long totalOutstandingAmount = memberOutstandingAmount + tempOutstandingAmount;

        if (Objects.equals(participantMemberId, bill.getPayerMemberId()) && tempOutstandingAmount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "垫付人无需结算");
        }
        if (totalOutstandingAmount <= 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该成员在当前账单中已结清");
        }

        LocalDateTime now = LocalDateTime.now();
        if (memberOutstandingAmount > 0) {
            PaymentConfirmRecord record = paymentConfirmRecordRepository.save(PaymentConfirmRecord.builder()
                .bookId(bookId)
                .fromMemberId(participantMemberId)
                .toMemberId(bill.getPayerMemberId())
                .paymentAmountCent(memberOutstandingAmount)
                .sourceType(PaymentSourceType.MANUAL)
                .sourceRefId(billId)
                .confirmStatus(PaymentConfirmStatus.CONFIRMED)
                .initiatedByMemberId(context.getCurrentMember().getId())
                .confirmedByMemberId(context.getCurrentMember().getId())
                .initiatedAt(now)
                .confirmedAt(now)
                .autoConfirmAt(null)
                .remark(StringUtils.hasText(remark) ? remark.trim() : "账单详情页成员结算")
                .build());
            paymentConfirmAllocationRepository.saveAll(record.getId(), List.of(PaymentConfirmAllocation.builder()
                .paymentConfirmId(record.getId())
                .billId(billId)
                .allocatedAmountCent(memberOutstandingAmount)
                .build()));
        }
        for (TempSettleDraft draft : tempSettleDrafts) {
            TempRecoveryRecord recoveryRecord = tempRecoveryRecordRepository.save(TempRecoveryRecord.builder()
                .bookId(bookId)
                .tempParticipantId(draft.tempParticipantId())
                .attachedMemberId(participantMemberId)
                .recoveryAmountCent(draft.outstandingAmountCent())
                .confirmStatus(RecoveryConfirmStatus.CONFIRMED)
                .confirmedByMemberId(context.getCurrentMember().getId())
                .createdAt(now)
                .confirmedAt(now)
                .remark(StringUtils.hasText(remark) ? remark.trim() : "账单详情页成员代临时成员结算")
                .build());
            tempRecoveryAllocationRepository.saveAll(recoveryRecord.getId(), List.of(TempRecoveryAllocation.builder()
                .recoveryRecordId(recoveryRecord.getId())
                .billId(billId)
                .allocatedAmountCent(draft.outstandingAmountCent())
                .build()));
        }

        return BillOperationResult.builder()
            .billId(billId)
            .message("结算成功")
            .build();
    }

    private record TempSettleDraft(Long tempParticipantId, long outstandingAmountCent) {}

    @Transactional
    public BillOperationResult createPersonalBill(CreatePersonalBillCommand command) {
        validateCreatePersonalBillCommand(command);
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(command.getCurrentUserId(), command.getBookId());
        Book book = context.getBook();
        BillType billType = parseBillType(command.getBillType());

        if (billType == BillType.SHARED_EXPENSE) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "个人账单接口不支持创建均摊账单");
        }
        if (!book.isShared() && billType == BillType.PERSONAL_CARRY) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "个人账本不支持创建个人帮带账单");
        }

        BookMember payerMember = requireActiveMemberById(command.getBookId(), command.getPayerMemberId(), "付款成员不存在");
        BookMember recorderMember = requireActiveMemberById(command.getBookId(), command.getRecorderMemberId(), "记账成员不存在");
        ensurePersonalBillMembersAreCurrent(context.getCurrentMember(), payerMember, recorderMember);
        BookCategory category = requireActiveCategory(command.getBookId(), command.getCurrentUserId(), command.getCategoryId(), billType);
        TempParticipant targetTempParticipant = null;
        if (billType == BillType.PERSONAL_CARRY) {
            targetTempParticipant = requireTempParticipantForPersonalCarry(
                command.getBookId(),
                command.getTempParticipantId(),
                context.getCurrentMember(),
                "帮带对象不存在或当前用户不可见"
            );
        }

        Bill savedBill = billRepository.save(Bill.builder()
            .bookId(command.getBookId())
            .billType(billType)
            .title(command.getTitle().trim())
            .billAmountCent(command.getBillAmountCent())
            .categoryId(category.getId())
            .payerMemberId(payerMember.getId())
            .recorderMemberId(recorderMember.getId())
            .targetTempParticipantId(targetTempParticipant == null ? null : targetTempParticipant.getId())
            .billTime(command.getBillTime())
            .remark(normalizeText(command.getRemark()))
            .changeFlowStatus(BillChangeFlowStatus.NONE)
            .latestChangeRequestId(null)
            .hasChangeHistory(false)
            .status(BillStatus.ACTIVE)
            .build());
        billAttachmentRepository.replaceAll(savedBill.getId(), context.getCurrentMember().getId(), command.getAttachmentUrls());
        operationLogService.log(
            command.getBookId(),
            context.getCurrentMember().getUserId(),
            context.getCurrentMember().getId(),
            OperationType.CREATE_BILL,
            OperationTargetType.BILL,
            savedBill.getId(),
            null,
            "type=" + billType.getCode() + ",amount=" + savedBill.getBillAmountCent()
        );

        return BillOperationResult.builder()
            .billId(savedBill.getId())
            .message("个人账单创建成功")
            .build();
    }

    @Transactional
    public BillOperationResult createSharedExpenseBill(CreateSharedExpenseBillCommand command) {
        validateCreateSharedExpenseBillCommand(command);
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(command.getCurrentUserId(), command.getBookId());
        Book book = context.getBook();
        if (!book.isShared()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "个人账本不支持创建均摊账单");
        }

        BookMember payerMember = requireActiveMemberById(command.getBookId(), command.getPayerMemberId(), "付款成员不存在");
        BookMember recorderMember = requireActiveMemberById(command.getBookId(), command.getRecorderMemberId(), "记账成员不存在");
        BookCategory category = requireActiveCategory(command.getBookId(), command.getCurrentUserId(), command.getCategoryId(), BillType.SHARED_EXPENSE);
        List<BillShareItem> shareItems = buildValidatedShareItems(command.getBookId(), context.getCurrentMember(), payerMember, command.getShareItems());

        long sumAmount = shareItems.stream().mapToLong(BillShareItem::getShareAmountCent).sum();
        if (!Objects.equals(sumAmount, command.getBillAmountCent())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "分摊项金额之和必须与账单总金额完全一致");
        }

        Bill savedBill = billRepository.save(Bill.builder()
            .bookId(command.getBookId())
            .billType(BillType.SHARED_EXPENSE)
            .title(command.getTitle().trim())
            .billAmountCent(command.getBillAmountCent())
            .categoryId(category.getId())
            .payerMemberId(payerMember.getId())
            .recorderMemberId(recorderMember.getId())
            .targetTempParticipantId(null)
            .billTime(command.getBillTime())
            .remark(normalizeText(command.getRemark()))
            .changeFlowStatus(BillChangeFlowStatus.NONE)
            .latestChangeRequestId(null)
            .hasChangeHistory(false)
            .status(BillStatus.ACTIVE)
            .build());
        billShareItemRepository.saveAll(savedBill.getId(), shareItems);
        billAttachmentRepository.replaceAll(savedBill.getId(), context.getCurrentMember().getId(), command.getAttachmentUrls());
        operationLogService.log(
            command.getBookId(),
            context.getCurrentMember().getUserId(),
            context.getCurrentMember().getId(),
            OperationType.CREATE_BILL,
            OperationTargetType.BILL,
            savedBill.getId(),
            null,
            "type=SHARED_EXPENSE,amount=" + savedBill.getBillAmountCent() + ",shareItems=" + shareItems.size()
        );

        return BillOperationResult.builder()
            .billId(savedBill.getId())
            .message("均摊账单创建成功")
            .build();
    }

    @Transactional
    public BillOperationResult updateBill(UpdateBillCommand command) {
        validateUpdateBillCommand(command);
        VisibleBillAggregate visibleBill = billingAccessSupportService.requireVisibleBill(command.getBookId(), command.getBillId(), command.getCurrentUserId());
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(command.getCurrentUserId(), command.getBookId());
        Bill existingBill = visibleBill.getBill();

        if (!canDirectOperate(context.getCurrentMember(), existingBill, visibleBill.getShareItems())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有账单付款人或当前可见的记账人可以直接修改账单");
        }
        ensureNoConfirmedFlow(command.getBookId(), existingBill.getId());

        BillType nextBillType = parseBillType(command.getBillType());
        if (!context.getBook().isShared() && nextBillType == BillType.PERSONAL_CARRY) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "个人账本不支持个人帮带账单");
        }

        BookMember payerMember = requireActiveMemberById(command.getBookId(), command.getPayerMemberId(), "付款成员不存在");
        BookMember recorderMember = requireActiveMemberById(command.getBookId(), command.getRecorderMemberId(), "记账成员不存在");
        BookCategory category = requireActiveCategory(command.getBookId(), command.getCurrentUserId(), command.getCategoryId(), nextBillType);
        if (nextBillType != BillType.SHARED_EXPENSE) {
            ensurePersonalBillMembersAreCurrent(context.getCurrentMember(), payerMember, recorderMember);
        }

        Long targetTempParticipantId = null;
        List<BillShareItem> shareItems = List.of();
        if (nextBillType == BillType.PERSONAL_CARRY) {
            TempParticipant tempParticipant = requireTempParticipantForPersonalCarry(
                command.getBookId(),
                command.getTempParticipantId(),
                context.getCurrentMember(),
                "帮带对象不存在或当前用户不可见"
            );
            targetTempParticipantId = tempParticipant.getId();
        } else if (nextBillType == BillType.SHARED_EXPENSE) {
            if (!context.getBook().isShared()) {
                throw new BusinessException(ErrorCode.INVALID_PARAM, "个人账本不支持创建均摊账单");
            }
            shareItems = buildValidatedShareItems(command.getBookId(), context.getCurrentMember(), payerMember, command.getShareItems());
            long sumAmount = shareItems.stream().mapToLong(BillShareItem::getShareAmountCent).sum();
            if (!Objects.equals(sumAmount, command.getBillAmountCent())) {
                throw new BusinessException(ErrorCode.INVALID_PARAM, "分摊项金额之和必须与账单总金额完全一致");
            }
        }

        Bill updatedBill = existingBill.update(
            nextBillType,
            command.getTitle(),
            command.getBillAmountCent(),
            category.getId(),
            payerMember.getId(),
            recorderMember.getId(),
            targetTempParticipantId,
            command.getBillTime(),
            command.getRemark()
        );
        List<String> attachmentUrls = normalizeAttachmentUrls(command.getAttachmentUrls());
        billRepository.save(updatedBill);
        billShareItemRepository.deleteByBillId(existingBill.getId());
        if (nextBillType == BillType.SHARED_EXPENSE) {
            billShareItemRepository.saveAll(existingBill.getId(), shareItems);
        }
        billAttachmentRepository.replaceAll(existingBill.getId(), context.getCurrentMember().getId(), attachmentUrls);
        if (!command.isFromChangeRequest()) {
            recordDirectChangeRequest(
                existingBill,
                updatedBill,
                context.getCurrentMember().getId(),
                ChangeRequestType.MODIFY,
                normalizeText(command.getRemark()),
                attachmentUrls,
                shareItems
            );
        }
        operationLogService.log(
            command.getBookId(),
            context.getCurrentMember().getUserId(),
            context.getCurrentMember().getId(),
            OperationType.UPDATE_BILL,
            OperationTargetType.BILL,
            updatedBill.getId(),
            "type=" + existingBill.getBillType().getCode() + ",amount=" + existingBill.getBillAmountCent(),
            "type=" + updatedBill.getBillType().getCode() + ",amount=" + updatedBill.getBillAmountCent()
        );

        return BillOperationResult.builder()
            .billId(existingBill.getId())
            .message("账单更新成功")
            .build();
    }

    @Transactional
    public BillOperationResult deleteBill(DeleteBillCommand command) {
        if (command.getBillId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "billId 不能为空");
        }
        VisibleBillAggregate visibleBill = billingAccessSupportService.requireVisibleBill(command.getBookId(), command.getBillId(), command.getCurrentUserId());
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(command.getCurrentUserId(), command.getBookId());
        Bill bill = visibleBill.getBill();
        if (!canDirectOperate(context.getCurrentMember(), bill, visibleBill.getShareItems())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有账单付款人或当前可见的记账人可以直接删除账单");
        }
        ensureNoConfirmedFlow(command.getBookId(), bill.getId());

        billRepository.save(bill.delete());
        billAttachmentRepository.replaceAll(bill.getId(), null, List.of());
        if (!command.isFromChangeRequest()) {
            recordDirectChangeRequest(
                bill,
                bill.delete(),
                context.getCurrentMember().getId(),
                ChangeRequestType.DELETE,
                "直接删除账单",
                List.of(),
                List.of()
            );
        }
        operationLogService.log(
            command.getBookId(),
            context.getCurrentMember().getUserId(),
            context.getCurrentMember().getId(),
            OperationType.DELETE_BILL,
            OperationTargetType.BILL,
            bill.getId(),
            "status=" + bill.getStatus().getCode(),
            "status=" + BillStatus.DELETED.getCode()
        );
        return BillOperationResult.builder()
            .billId(bill.getId())
            .message("账单删除成功")
            .build();
    }

    private BillListItemResult toBillListItemResult(
        BookMember currentMember,
        Bill bill,
        List<BillShareItem> shareItems,
        BillingProjection projection,
        Map<Long, BookCategory> categoryMap
    ) {
        BookCategory category = categoryMap.get(bill.getCategoryId());
        long viewerReceivable = calculateViewerReceivable(currentMember, bill, projection);
        long viewerRecovered = calculateViewerRecovered(currentMember, bill, projection);
        long viewerOwnShare = calculateViewerOwnShare(currentMember, bill, shareItems);
        long viewerOwnPaid = calculateViewerOwnPaid(currentMember, bill, projection);
        long viewerAttachedTempShare = calculateViewerAttachedTempShare(currentMember, bill, shareItems);
        long viewerAttachedTempRecovered = calculateViewerAttachedTempRecovered(currentMember, bill, projection);
        long viewerReceivableTotal = viewerReceivable + (bill.isSharedExpense() ? viewerAttachedTempShare : 0L);
        long viewerRecoveredTotal = viewerRecovered + (bill.isSharedExpense() ? viewerAttachedTempRecovered : 0L);

        return BillListItemResult.builder()
            .billId(bill.getId())
            .billType(bill.getBillType().getCode())
            .title(bill.getTitle())
            .billAmountCent(bill.getBillAmountCent())
            .categoryId(bill.getCategoryId())
            .categoryName(category == null ? null : category.getName())
            .categoryIcon(category == null ? null : category.getIcon())
            .billTime(bill.getBillTime())
            .createdAt(bill.getCreatedAt())
            .viewerIsPayer(Objects.equals(currentMember.getId(), bill.getPayerMemberId()))
            .viewerIsRecorder(Objects.equals(currentMember.getId(), bill.getRecorderMemberId()))
            .viewerHasAttachedTempShare(viewerAttachedTempShare > 0)
            .directEditable(canDirectOperate(currentMember, bill, shareItems))
            .directDeletable(canDirectOperate(currentMember, bill, shareItems))
            .viewerOwnShareAmountCent(viewerOwnShare)
            .viewerOwnPaidAmountCent(viewerOwnPaid)
            .viewerOwnUnpaidAmountCent(Math.max(viewerOwnShare - viewerOwnPaid, 0L))
            .viewerAttachedTempShareAmountCent(viewerAttachedTempShare)
            .viewerAttachedTempRecoveredAmountCent(viewerAttachedTempRecovered)
            .viewerAttachedTempUnrecoveredAmountCent(Math.max(viewerAttachedTempShare - viewerAttachedTempRecovered, 0L))
            .viewerReceivableAmountCent(viewerReceivableTotal)
            .viewerRecoveredAmountCent(viewerRecoveredTotal)
            .viewerUnrecoveredAmountCent(Math.max(viewerReceivableTotal - viewerRecoveredTotal, 0L))
            .build();
    }

    private Map<Long, BookCategory> loadCategoryMap(Long bookId, Long currentUserId) {
        return bookCategoryRepository.findVisibleForBook(bookId, currentUserId).stream()
            .collect(Collectors.toMap(BookCategory::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, BookMember> loadRelatedMemberMap(Long bookId, Bill bill, List<BillShareItem> shareItems) {
        Map<Long, BookMember> activeMemberMap = bookMemberRepository.findActiveByBookId(bookId).stream()
            .collect(Collectors.toMap(BookMember::getId, Function.identity(), (left, right) -> left));

        List<Long> candidateIds = new ArrayList<>();
        candidateIds.add(bill.getPayerMemberId());
        candidateIds.add(bill.getRecorderMemberId());
        shareItems.forEach(item -> {
            if (item.getParticipantType() == ShareParticipantType.MEMBER) {
                candidateIds.add(item.getParticipantRefId());
            }
            if (item.getAttachedMemberId() != null) {
                candidateIds.add(item.getAttachedMemberId());
            }
        });

        candidateIds.stream()
            .filter(Objects::nonNull)
            .filter(memberId -> !activeMemberMap.containsKey(memberId))
            .distinct()
            .forEach(memberId -> bookMemberRepository.findById(memberId).ifPresent(member -> activeMemberMap.put(memberId, member)));
        return activeMemberMap;
    }

    private Map<Long, BookUserSummary> loadUserSummaryMap(Collection<BookMember> members) {
        return bookUserDirectory.getByUserIds(members.stream()
            .map(BookMember::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .toList());
    }

    private Map<Long, TempParticipant> loadTempParticipantMap(Long bookId) {
        return tempParticipantRepository.findByBookId(bookId).stream()
            .collect(Collectors.toMap(TempParticipant::getId, Function.identity(), (left, right) -> left));
    }

    private BillDetailResult.MemberSummaryResult toMemberSummaryResult(
        BookMember member,
        Map<Long, BookUserSummary> userSummaryMap
    ) {
        if (member == null) {
            return null;
        }
        BookUserSummary summary = userSummaryMap.get(member.getUserId());
        return BillDetailResult.MemberSummaryResult.builder()
            .memberId(member.getId())
            .userId(member.getUserId())
            .nickname(summary == null ? null : summary.getNickname())
            .avatarUrl(summary == null ? null : summary.getAvatarUrl())
            .phoneNumber(summary == null ? null : summary.getPhoneNumber())
            .build();
    }

    private BillDetailResult.TempSummaryResult toTempSummaryResult(
        TempParticipant tempParticipant,
        Map<Long, BookMember> memberMap,
        Map<Long, BookUserSummary> userSummaryMap
    ) {
        if (tempParticipant == null) {
            return null;
        }
        BookMember attachedMember = memberMap.get(tempParticipant.getAttachedMemberId());
        BookUserSummary attachedSummary = attachedMember == null ? null : userSummaryMap.get(attachedMember.getUserId());
        return BillDetailResult.TempSummaryResult.builder()
            .tempParticipantId(tempParticipant.getId())
            .nickname(tempParticipant.getNickname())
            .attachedMemberId(tempParticipant.getAttachedMemberId())
            .attachedMemberNickname(attachedSummary == null ? null : attachedSummary.getNickname())
            .build();
    }

    private List<BillDetailResult.ParticipantResult> toParticipantResults(
        Bill bill,
        List<BillShareItem> shareItems,
        BillingProjection projection,
        Map<Long, BookMember> memberMap,
        Map<Long, BookUserSummary> userSummaryMap,
        Map<Long, TempParticipant> tempParticipantMap
    ) {
        if (!bill.isSharedExpense()) {
            return List.of();
        }
        List<BillDetailResult.ParticipantResult> results = new ArrayList<>();
        for (BillShareItem shareItem : shareItems) {
            if (shareItem.getParticipantType() == ShareParticipantType.MEMBER) {
                BookMember member = memberMap.get(shareItem.getParticipantRefId());
                BookUserSummary summary = member == null ? null : userSummaryMap.get(member.getUserId());
                long confirmed = Objects.equals(shareItem.getParticipantRefId(), bill.getPayerMemberId())
                    ? shareItem.getShareAmountCent()
                    : calculateConfirmedPaymentForBill(
                        projection,
                        bill.getId(),
                        shareItem.getParticipantRefId(),
                        bill.getPayerMemberId()
                    );
                results.add(BillDetailResult.ParticipantResult.builder()
                    .participantType(shareItem.getParticipantType().getCode())
                    .participantRefId(shareItem.getParticipantRefId())
                    .nickname(summary == null ? null : summary.getNickname())
                    .avatarUrl(summary == null ? null : summary.getAvatarUrl())
                    .attachedMemberId(null)
                    .attachedMemberNickname(null)
                    .shareAmountCent(shareItem.getShareAmountCent())
                    .confirmedAmountCent(Math.min(confirmed, shareItem.getShareAmountCent()))
                    .unconfirmedAmountCent(Math.max(shareItem.getShareAmountCent() - confirmed, 0L))
                    .build());
                continue;
            }

            TempParticipant tempParticipant = tempParticipantMap.get(shareItem.getParticipantRefId());
            BookMember attachedMember = tempParticipant == null ? null : memberMap.get(tempParticipant.getAttachedMemberId());
            BookUserSummary attachedSummary = attachedMember == null ? null : userSummaryMap.get(attachedMember.getUserId());
            long confirmed = tempParticipant == null
                ? 0L
                : calculateConfirmedRecoveryForBill(
                    projection,
                    bill.getId(),
                    tempParticipant.getId(),
                    tempParticipant.getAttachedMemberId()
                );
            results.add(BillDetailResult.ParticipantResult.builder()
                .participantType(shareItem.getParticipantType().getCode())
                .participantRefId(shareItem.getParticipantRefId())
                .nickname(tempParticipant == null ? null : tempParticipant.getNickname())
                .avatarUrl(null)
                .attachedMemberId(tempParticipant == null ? null : tempParticipant.getAttachedMemberId())
                .attachedMemberNickname(attachedSummary == null ? null : attachedSummary.getNickname())
                .shareAmountCent(shareItem.getShareAmountCent())
                .confirmedAmountCent(Math.min(confirmed, shareItem.getShareAmountCent()))
                .unconfirmedAmountCent(Math.max(shareItem.getShareAmountCent() - confirmed, 0L))
                .build());
        }
        return results;
    }

    private long calculateViewerReceivable(BookMember currentMember, Bill bill, BillingProjection projection) {
        if (bill.isPersonalCarry()) {
            return Objects.equals(currentMember.getId(), bill.getPayerMemberId()) ? bill.getBillAmountCent() : 0L;
        }
        if (!bill.isSharedExpense()) {
            return 0L;
        }
        return projection.getSharedDebtLines().stream()
            .filter(line -> Objects.equals(line.getBillId(), bill.getId()))
            .filter(line -> Objects.equals(line.getToMemberId(), currentMember.getId()))
            .mapToLong(line -> Math.max(line.getAmountCent(), 0L))
            .sum();
    }

    private long calculateViewerRecovered(BookMember currentMember, Bill bill, BillingProjection projection) {
        if (bill.isPersonalCarry()) {
            if (!Objects.equals(currentMember.getId(), bill.getPayerMemberId()) || bill.getTargetTempParticipantId() == null) {
                return 0L;
            }
            TempParticipant carryTarget = tempParticipantRepository.findById(bill.getTargetTempParticipantId())
                .filter(item -> Objects.equals(item.getBookId(), bill.getBookId()))
                .orElse(null);
            if (carryTarget == null) {
                return 0L;
            }
            if (Objects.equals(carryTarget.getAttachedMemberId(), currentMember.getId())) {
                return calculateConfirmedRecoveryForBill(projection, bill.getId(), carryTarget.getId(), currentMember.getId());
            }
            if (carryTarget.getAttachedMemberId() == null) {
                return 0L;
            }
            long confirmed = calculateConfirmedPaymentForBill(
                projection,
                bill.getId(),
                carryTarget.getAttachedMemberId(),
                currentMember.getId()
            );
            return Math.min(confirmed, Math.max(bill.getBillAmountCent(), 0L));
        }
        if (!bill.isSharedExpense()) {
            return 0L;
        }
        Map<Long, Long> receivableByFromMember = projection.getSharedDebtLines().stream()
            .filter(line -> Objects.equals(line.getBillId(), bill.getId()))
            .filter(line -> Objects.equals(line.getToMemberId(), currentMember.getId()))
            .collect(Collectors.toMap(
                SharedDebtLine::getFromMemberId,
                line -> Math.max(line.getAmountCent(), 0L),
                Long::sum
            ));
        return receivableByFromMember.entrySet().stream()
            .mapToLong(entry -> Math.min(
                calculateConfirmedPaymentForBill(projection, bill.getId(), entry.getKey(), currentMember.getId()),
                entry.getValue()
            ))
            .sum();
    }

    private long calculateViewerOwnShare(BookMember currentMember, Bill bill, List<BillShareItem> shareItems) {
        if (!bill.isSharedExpense()) {
            return 0L;
        }
        return shareItems.stream()
            .filter(item -> item.getParticipantType() == ShareParticipantType.MEMBER)
            .filter(item -> Objects.equals(item.getParticipantRefId(), currentMember.getId()))
            .mapToLong(BillShareItem::getShareAmountCent)
            .sum();
    }

    private long calculateViewerOwnPaid(BookMember currentMember, Bill bill, BillingProjection projection) {
        if (!bill.isSharedExpense()) {
            return 0L;
        }
        if (Objects.equals(currentMember.getId(), bill.getPayerMemberId())) {
            return calculateViewerOwnShare(currentMember, bill, projection.getShareItemsByBillId().getOrDefault(bill.getId(), List.of()));
        }
        long ownShare = projection.getShareItemsByBillId().getOrDefault(bill.getId(), List.of()).stream()
            .filter(item -> item.getParticipantType() == ShareParticipantType.MEMBER)
            .filter(item -> Objects.equals(item.getParticipantRefId(), currentMember.getId()))
            .mapToLong(BillShareItem::getShareAmountCent)
            .sum();
        long paid = calculateConfirmedPaymentForBill(projection, bill.getId(), currentMember.getId(), bill.getPayerMemberId());
        return Math.min(ownShare, paid);
    }

    private long calculateViewerAttachedTempShare(BookMember currentMember, Bill bill, List<BillShareItem> shareItems) {
        if (!bill.isSharedExpense()) {
            return 0L;
        }
        return shareItems.stream()
            .filter(item -> item.getParticipantType() == ShareParticipantType.TEMP_PARTICIPANT)
            .filter(item -> Objects.equals(item.getAttachedMemberId(), currentMember.getId()))
            .mapToLong(BillShareItem::getShareAmountCent)
            .sum();
    }

    private long calculateViewerAttachedTempRecovered(BookMember currentMember, Bill bill, BillingProjection projection) {
        if (bill.isPersonalCarry()) {
            if (!Objects.equals(currentMember.getId(), bill.getPayerMemberId()) || bill.getTargetTempParticipantId() == null) {
                return 0L;
            }
            return calculateConfirmedRecoveryForBill(projection, bill.getId(), bill.getTargetTempParticipantId(), currentMember.getId());
        }
        if (!bill.isSharedExpense()) {
            return 0L;
        }
        return projection.getTempRecoveryAllocationViews().stream()
            .filter(view -> Objects.equals(view.getBillId(), bill.getId()))
            .filter(view -> Objects.equals(view.getAttachedMemberId(), currentMember.getId()))
            .mapToLong(TempRecoveryAllocationView::getAllocatedAmountCent)
            .sum();
    }

    private long calculateConfirmedPaymentForBill(
        BillingProjection projection,
        Long billId,
        Long fromMemberId,
        Long toMemberId
    ) {
        return projection.getPaymentAllocationViews().stream()
            .filter(view -> Objects.equals(view.getBillId(), billId))
            .filter(view -> Objects.equals(view.getFromMemberId(), fromMemberId))
            .filter(view -> Objects.equals(view.getToMemberId(), toMemberId))
            .filter(view -> view.getConfirmStatus() != null && (
                "CONFIRMED".equals(view.getConfirmStatus().name()) || "AUTO_CONFIRMED".equals(view.getConfirmStatus().name())
            ))
            .mapToLong(view -> view.getAllocatedAmountCent())
            .sum();
    }

    private long calculateConfirmedRecoveryForBill(
        BillingProjection projection,
        Long billId,
        Long tempParticipantId,
        Long attachedMemberId
    ) {
        return projection.getTempRecoveryAllocationViews().stream()
            .filter(view -> Objects.equals(view.getBillId(), billId))
            .filter(view -> Objects.equals(view.getTempParticipantId(), tempParticipantId))
            .filter(view -> Objects.equals(view.getAttachedMemberId(), attachedMemberId))
            .mapToLong(TempRecoveryAllocationView::getAllocatedAmountCent)
            .sum();
    }

    private boolean canDirectOperate(BookMember currentMember, Bill bill, List<BillShareItem> shareItems) {
        return Objects.equals(currentMember.getId(), bill.getPayerMemberId())
            || (Objects.equals(currentMember.getId(), bill.getRecorderMemberId())
                && billingAccessSupportService.canViewBill(currentMember, bill, shareItems));
    }

    private BookMember requireActiveMemberById(Long bookId, Long memberId, String errorMessage) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "成员 id 不能为空");
        }
        return bookMemberRepository.findById(memberId)
            .filter(BookMember::isActive)
            .filter(member -> Objects.equals(member.getBookId(), bookId))
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, errorMessage));
    }

    private BookCategory requireActiveCategory(Long bookId, Long currentUserId, Long categoryId, BillType billType) {
        if (categoryId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "categoryId 不能为空");
        }
        Map<Long, BookCategory> visibleCategoryMap = loadCategoryMap(bookId, currentUserId);
        BookCategory category = visibleCategoryMap.get(categoryId);
        if (category == null || !category.isActive()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在或当前用户不可用");
        }
        CategoryType expectedType = billType == BillType.PERSONAL_INCOME ? CategoryType.INCOME : CategoryType.EXPENSE;
        if (category.getCategoryType() != expectedType) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "账单类型与分类类型不匹配");
        }
        if (billType == BillType.SHARED_EXPENSE && category.isLocalCustom()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "共享账单仅支持系统分类或账本级分类");
        }
        return category;
    }

    private void ensurePersonalBillMembersAreCurrent(BookMember currentMember, BookMember payerMember, BookMember recorderMember) {
        if (!Objects.equals(payerMember.getId(), currentMember.getId())
            || !Objects.equals(recorderMember.getId(), currentMember.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "个人账单只能由当前用户为本人创建或修改");
        }
    }

    private TempParticipant requireTempParticipantForPersonalCarry(
        Long bookId,
        Long tempParticipantId,
        BookMember currentMember,
        String errorMessage
    ) {
        if (tempParticipantId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "tempParticipantId 不能为空");
        }
        TempParticipant tempParticipant = tempParticipantRepository.findById(tempParticipantId)
            .filter(item -> Objects.equals(item.getBookId(), bookId))
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, errorMessage));
        if (!tempParticipant.isActive()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "该临时成员当前已禁用");
        }
        if (TempParticipantType.normalize(tempParticipant.getTempType()) == TempParticipantType.PRIVATE
            && !Objects.equals(tempParticipant.getAttachedMemberId(), currentMember.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, errorMessage);
        }
        return tempParticipant;
    }

    private TempParticipant requireTempParticipantForSharedExpense(Long bookId, Long tempParticipantId, String errorMessage) {
        if (tempParticipantId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "participantRefId 不能为空");
        }
        TempParticipant tempParticipant = tempParticipantRepository.findById(tempParticipantId)
            .filter(item -> Objects.equals(item.getBookId(), bookId))
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, errorMessage));
        if (!tempParticipant.isActive()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "该临时成员当前已禁用");
        }
        if (TempParticipantType.normalize(tempParticipant.getTempType()) != TempParticipantType.GLOBAL) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "私有临时成员不能用于共享账单");
        }
        return tempParticipant;
    }

    private List<BillShareItem> buildValidatedShareItems(
        Long bookId,
        BookMember currentMember,
        BookMember payerMember,
        List<BillShareItemCommand> shareItemCommands
    ) {
        if (shareItemCommands == null || shareItemCommands.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "均摊账单至少需要一个分摊对象");
        }
        Set<String> duplicateCheck = new java.util.HashSet<>();
        String unifiedShareMethod = null;
        List<BillShareItem> shareItems = new ArrayList<>();

        for (BillShareItemCommand itemCommand : shareItemCommands) {
            if (itemCommand == null) {
                throw new BusinessException(ErrorCode.INVALID_PARAM, "分摊项不能为空");
            }
            ShareParticipantType participantType = parseShareParticipantType(itemCommand.getParticipantType());
            ShareMethod shareMethod = parseShareMethod(itemCommand.getShareMethod());
            if (unifiedShareMethod == null) {
                unifiedShareMethod = shareMethod.getCode();
            } else if (!Objects.equals(unifiedShareMethod, shareMethod.getCode())) {
                throw new BusinessException(ErrorCode.INVALID_PARAM, "同一笔均摊账单仅支持使用一种分摊方式");
            }
            if (itemCommand.getParticipantRefId() == null) {
                throw new BusinessException(ErrorCode.INVALID_PARAM, "participantRefId 不能为空");
            }
            if (itemCommand.getShareAmountCent() == null || itemCommand.getShareAmountCent() <= 0) {
                throw new BusinessException(ErrorCode.INVALID_PARAM, "shareAmountCent 必须大于 0");
            }
            if (shareMethod == ShareMethod.RATIO && itemCommand.getShareRatio() == null) {
                throw new BusinessException(ErrorCode.INVALID_PARAM, "按比例分摊时 shareRatio 不能为空");
            }
            if (shareMethod == ShareMethod.RATIO && itemCommand.getShareRatio().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.INVALID_PARAM, "按比例分摊时 shareRatio 必须大于 0");
            }
            String duplicateKey = participantType.getCode() + ":" + itemCommand.getParticipantRefId();
            if (!duplicateCheck.add(duplicateKey)) {
                throw new BusinessException(ErrorCode.CONFLICT, "同一分摊对象不能在一笔账单中重复出现");
            }

            if (participantType == ShareParticipantType.MEMBER) {
                BookMember targetMember = requireActiveMemberById(bookId, itemCommand.getParticipantRefId(), "分摊正式成员不存在");
                shareItems.add(BillShareItem.builder()
                    .participantType(participantType)
                    .participantRefId(targetMember.getId())
                    .attachedMemberId(null)
                    .shareMethod(shareMethod)
                    .shareRatio(normalizeShareRatio(itemCommand.getShareRatio()))
                    .shareAmountCent(itemCommand.getShareAmountCent())
                    .build());
                continue;
            }

            TempParticipant tempParticipant = requireTempParticipantForSharedExpense(
                bookId,
                itemCommand.getParticipantRefId(),
                "分摊临时成员不存在或当前用户不可见"
            );
            shareItems.add(BillShareItem.builder()
                .participantType(participantType)
                .participantRefId(tempParticipant.getId())
                .attachedMemberId(tempParticipant.getAttachedMemberId())
                .shareMethod(shareMethod)
                .shareRatio(normalizeShareRatio(itemCommand.getShareRatio()))
                .shareAmountCent(itemCommand.getShareAmountCent())
                .build());
        }

        boolean payerParticipates = shareItems.stream()
            .anyMatch(item -> item.getParticipantType() == ShareParticipantType.MEMBER
                && Objects.equals(item.getParticipantRefId(), payerMember.getId()));
        if (!payerParticipates) {
            // 付款人可以不参与分摊，这里仅保留注释作为明确业务意图。
        }
        return shareItems;
    }

    private void validateCreatePersonalBillCommand(CreatePersonalBillCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        if (command.getBookId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "bookId 不能为空");
        }
        if (!StringUtils.hasText(command.getBillType())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "billType 不能为空");
        }
        validateBaseBillFields(
            command.getTitle(),
            command.getBillAmountCent(),
            command.getCategoryId(),
            command.getPayerMemberId(),
            command.getRecorderMemberId(),
            command.getBillTime()
        );
    }

    private void validateCreateSharedExpenseBillCommand(CreateSharedExpenseBillCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        if (command.getBookId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "bookId 不能为空");
        }
        validateBaseBillFields(
            command.getTitle(),
            command.getBillAmountCent(),
            command.getCategoryId(),
            command.getPayerMemberId(),
            command.getRecorderMemberId(),
            command.getBillTime()
        );
    }

    private void validateUpdateBillCommand(UpdateBillCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        if (command.getBookId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "bookId 不能为空");
        }
        if (command.getBillId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "billId 不能为空");
        }
        if (!StringUtils.hasText(command.getBillType())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "billType 不能为空");
        }
        validateBaseBillFields(
            command.getTitle(),
            command.getBillAmountCent(),
            command.getCategoryId(),
            command.getPayerMemberId(),
            command.getRecorderMemberId(),
            command.getBillTime()
        );
    }

    private void validateBaseBillFields(
        String title,
        Long billAmountCent,
        Long categoryId,
        Long payerMemberId,
        Long recorderMemberId,
        LocalDateTime billTime
    ) {
        if (!StringUtils.hasText(title)) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "账单标题不能为空");
        }
        if (billAmountCent == null || billAmountCent <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "账单金额必须大于 0");
        }
        if (categoryId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "categoryId 不能为空");
        }
        if (payerMemberId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "payerMemberId 不能为空");
        }
        if (recorderMemberId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "recorderMemberId 不能为空");
        }
        if (billTime == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "billTime 不能为空");
        }
    }

    private void validateCurrentUser(Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前登录信息不存在");
        }
    }

    private BillType parseBillType(String rawType) {
        try {
            return BillType.valueOf(rawType.trim().toUpperCase());
        } catch (Exception exception) {
            throw new BusinessException(
                ErrorCode.INVALID_PARAM,
                "billType 仅支持 PERSONAL_EXPENSE、PERSONAL_INCOME、PERSONAL_CARRY、SHARED_EXPENSE"
            );
        }
    }

    private ShareParticipantType parseShareParticipantType(String rawType) {
        try {
            return ShareParticipantType.valueOf(rawType.trim().toUpperCase());
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "participantType 仅支持 MEMBER 或 TEMP_PARTICIPANT");
        }
    }

    private ShareMethod parseShareMethod(String rawMethod) {
        try {
            return ShareMethod.valueOf(rawMethod.trim().toUpperCase());
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "shareMethod 仅支持 AVERAGE、FIXED_AMOUNT、RATIO");
        }
    }

    private BigDecimal normalizeShareRatio(BigDecimal shareRatio) {
        return shareRatio == null ? null : shareRatio.stripTrailingZeros();
    }

    private String normalizeText(String rawText) {
        return StringUtils.hasText(rawText) ? rawText.trim() : null;
    }

    private void recordDirectChangeRequest(
        Bill beforeBill,
        Bill afterBill,
        Long requesterMemberId,
        ChangeRequestType requestType,
        String requestReason,
        List<String> attachmentUrls,
        List<BillShareItem> shareItems
    ) {
        ensureBaselineSnapshot(beforeBill, requesterMemberId);

        LocalDateTime now = LocalDateTime.now();
        BillChangeRequest latest = loadLatestRequest(beforeBill.getId());
        BillChangeRequest savedRequest = billChangeRequestRepository.save(BillChangeRequest.builder()
            .billId(beforeBill.getId())
            .predecessorRequestId(latest == null ? null : latest.getId())
            .baseline(false)
            .requestType(requestType)
            .requesterMemberId(requesterMemberId)
            .requestReason(normalizeDirectReason(requestReason))
            .status(ChangeRequestStatus.APPROVED)
            .createdAt(now)
            .handledAt(now)
            .build());
        saveRequestSnapshot(
            savedRequest,
            afterBill,
            shareItems,
            attachmentUrls,
            requesterMemberId
        );
        refreshBillChangeFlowState(beforeBill.getId());
    }

    private void ensureBaselineSnapshot(Bill bill, Long requesterMemberId) {
        if (bill == null || billChangeRequestRepository.findByBillId(bill.getId()).stream().anyMatch(BillChangeRequest::isBaseline)) {
            return;
        }
        LocalDateTime baselineTime = bill.getCreatedAt() == null ? LocalDateTime.now() : bill.getCreatedAt();
        BillChangeRequest baseline = billChangeRequestRepository.save(BillChangeRequest.builder()
            .billId(bill.getId())
            .predecessorRequestId(null)
            .baseline(true)
            .requestType(ChangeRequestType.MODIFY)
            .requesterMemberId(requesterMemberId)
            .requestReason("原始账单快照")
            .status(ChangeRequestStatus.APPROVED)
            .createdAt(baselineTime)
            .handledAt(baselineTime)
            .build());
        saveRequestSnapshot(
            baseline,
            bill,
            bill.isSharedExpense() ? billShareItemRepository.findByBillId(bill.getId()) : List.of(),
            billAttachmentRepository.findByBillId(bill.getId()).stream()
                .map(BillAttachment::getFileUrl)
                .toList(),
            requesterMemberId
        );
    }

    private void saveRequestSnapshot(
        BillChangeRequest request,
        Bill snapshotBill,
        List<BillShareItem> shareItems,
        List<String> attachmentUrls,
        Long uploadedByMemberId
    ) {
        if (request == null || snapshotBill == null) {
            return;
        }
        billChangeRequestSnapshotRepository.save(BillChangeRequestSnapshot.builder()
            .requestId(request.getId())
            .billType(snapshotBill.getBillType().getCode())
            .title(snapshotBill.getTitle())
            .billAmountCent(snapshotBill.getBillAmountCent())
            .categoryId(snapshotBill.getCategoryId())
            .payerMemberId(snapshotBill.getPayerMemberId())
            .recorderMemberId(snapshotBill.getRecorderMemberId())
            .tempParticipantId(snapshotBill.getTargetTempParticipantId())
            .billTime(snapshotBill.getBillTime())
            .remark(snapshotBill.getRemark())
            .build());

        List<BillShareItem> snapshotShareItems = shareItems == null ? List.of() : shareItems;
        if (snapshotBill.isSharedExpense() && snapshotShareItems.isEmpty()) {
            snapshotShareItems = billShareItemRepository.findByBillId(snapshotBill.getId());
        }
        billChangeRequestShareItemRepository.saveAll(request.getId(), snapshotShareItems.stream()
            .map(item -> BillChangeRequestShareItem.builder()
                .requestId(request.getId())
                .participantType(item.getParticipantType())
                .participantRefId(item.getParticipantRefId())
                .attachedMemberId(item.getAttachedMemberId())
                .shareMethod(item.getShareMethod())
                .shareRatio(item.getShareRatio())
                .shareAmountCent(item.getShareAmountCent())
                .build())
            .toList());

        billChangeRequestAttachmentRepository.saveAll(request.getId(), normalizeAttachmentUrls(attachmentUrls).stream()
            .map(url -> BillChangeRequestAttachment.builder()
                .requestId(request.getId())
                .fileUrl(url)
                .uploadedByMemberId(uploadedByMemberId)
                .build())
            .toList());
    }

    private BillChangeRequest loadLatestRequest(Long billId) {
        List<BillChangeRequest> requests = billChangeRequestRepository.findByBillId(billId);
        return requests.isEmpty() ? null : requests.getFirst();
    }

    private void refreshBillChangeFlowState(Long billId) {
        Bill current = billRepository.findById(billId).orElse(null);
        if (current == null) {
            return;
        }
        List<BillChangeRequest> requests = billChangeRequestRepository.findByBillId(billId);
        BillChangeRequest latest = requests.isEmpty() ? null : requests.getFirst();
        BillChangeFlowStatus flowStatus = BillChangeFlowStatus.NONE;
        if (!requests.isEmpty()) {
            flowStatus = requests.stream().anyMatch(BillChangeRequest::isPending)
                ? BillChangeFlowStatus.PENDING
                : BillChangeFlowStatus.HISTORY;
        }
        billRepository.save(Bill.builder()
            .id(current.getId())
            .bookId(current.getBookId())
            .billType(current.getBillType())
            .title(current.getTitle())
            .billAmountCent(current.getBillAmountCent())
            .categoryId(current.getCategoryId())
            .payerMemberId(current.getPayerMemberId())
            .recorderMemberId(current.getRecorderMemberId())
            .targetTempParticipantId(current.getTargetTempParticipantId())
            .billTime(current.getBillTime())
            .remark(current.getRemark())
            .changeFlowStatus(flowStatus)
            .latestChangeRequestId(latest == null ? null : latest.getId())
            .hasChangeHistory(!requests.isEmpty())
            .status(current.getStatus())
            .createdAt(current.getCreatedAt())
            .updatedAt(current.getUpdatedAt())
            .build());
    }

    private List<String> normalizeAttachmentUrls(List<String> rawUrls) {
        if (rawUrls == null || rawUrls.isEmpty()) {
            return List.of();
        }
        return rawUrls.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .toList();
    }

    private String normalizeDirectReason(String rawReason) {
        if (!StringUtils.hasText(rawReason)) {
            return "直接操作";
        }
        return rawReason.trim();
    }

    private void ensureNoConfirmedFlow(Long bookId, Long billId) {
        BillingProjection projection = billingProjectionService.loadProjection(bookId);
        boolean hasPaymentFlow = projection.getPaymentAllocationViews().stream()
            .anyMatch(view -> Objects.equals(view.getBillId(), billId));
        boolean hasRecoveryFlow = projection.getTempRecoveryAllocationViews().stream()
            .anyMatch(view -> Objects.equals(view.getBillId(), billId));
        if (hasPaymentFlow || hasRecoveryFlow) {
            throw new BusinessException(
                ErrorCode.INVALID_STATUS,
                "该账单已有支付或回补历史记录，为保证数据可追溯性，暂不支持直接修改或删除"
            );
        }
    }
}
