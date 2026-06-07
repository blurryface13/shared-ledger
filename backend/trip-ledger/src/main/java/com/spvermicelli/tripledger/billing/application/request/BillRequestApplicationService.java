package com.spvermicelli.tripledger.billing.application.request;

import com.spvermicelli.tripledger.billing.application.bill.BillApplicationService;
import com.spvermicelli.tripledger.billing.application.bill.command.BillShareItemCommand;
import com.spvermicelli.tripledger.billing.application.bill.command.DeleteBillCommand;
import com.spvermicelli.tripledger.billing.application.bill.command.UpdateBillCommand;
import com.spvermicelli.tripledger.billing.application.request.command.ApproveBillRequestCommand;
import com.spvermicelli.tripledger.billing.application.request.command.CancelBillRequestCommand;
import com.spvermicelli.tripledger.billing.application.request.command.CreateDeleteBillRequestCommand;
import com.spvermicelli.tripledger.billing.application.request.command.CreateModifyBillRequestCommand;
import com.spvermicelli.tripledger.billing.application.request.command.RejectBillRequestCommand;
import com.spvermicelli.tripledger.billing.application.request.result.BillRequestDetailResult;
import com.spvermicelli.tripledger.billing.application.request.result.BillRequestOperationResult;
import com.spvermicelli.tripledger.billing.application.support.BillingAccessSupportService;
import com.spvermicelli.tripledger.billing.application.support.BillingBookMemberContext;
import com.spvermicelli.tripledger.billing.application.support.VisibleBillAggregate;
import com.spvermicelli.tripledger.billing.domain.bill.model.Bill;
import com.spvermicelli.tripledger.billing.domain.bill.model.BillAttachment;
import com.spvermicelli.tripledger.billing.domain.bill.model.BillShareItem;
import com.spvermicelli.tripledger.billing.domain.bill.repository.BillAttachmentRepository;
import com.spvermicelli.tripledger.billing.domain.bill.repository.BillRepository;
import com.spvermicelli.tripledger.billing.domain.bill.repository.BillShareItemRepository;
import com.spvermicelli.tripledger.billing.domain.request.model.BillChangeRequest;
import com.spvermicelli.tripledger.billing.domain.request.model.BillChangeRequestApproval;
import com.spvermicelli.tripledger.billing.domain.request.model.BillChangeRequestAttachment;
import com.spvermicelli.tripledger.billing.domain.request.model.BillChangeRequestShareItem;
import com.spvermicelli.tripledger.billing.domain.request.model.BillChangeRequestSnapshot;
import com.spvermicelli.tripledger.billing.domain.request.repository.BillChangeRequestApprovalRepository;
import com.spvermicelli.tripledger.billing.domain.request.repository.BillChangeRequestAttachmentRepository;
import com.spvermicelli.tripledger.billing.domain.request.repository.BillChangeRequestRepository;
import com.spvermicelli.tripledger.billing.domain.request.repository.BillChangeRequestShareItemRepository;
import com.spvermicelli.tripledger.billing.domain.request.repository.BillChangeRequestSnapshotRepository;
import com.spvermicelli.tripledger.ledger.domain.book.model.BookMember;
import com.spvermicelli.tripledger.ledger.domain.book.repository.BookMemberRepository;
import com.spvermicelli.tripledger.ledger.domain.book.service.BookUserDirectory;
import com.spvermicelli.tripledger.ledger.domain.book.valueobject.BookUserSummary;
import com.spvermicelli.tripledger.ledger.domain.category.model.BookCategory;
import com.spvermicelli.tripledger.ledger.domain.category.repository.BookCategoryRepository;
import com.spvermicelli.tripledger.ledger.domain.tempParticipant.model.TempParticipant;
import com.spvermicelli.tripledger.ledger.domain.tempParticipant.repository.TempParticipantRepository;
import com.spvermicelli.tripledger.shared.application.operation.OperationLogService;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.spvermicelli.tripledger.shared.domain.enums.ApprovalAction;
import com.spvermicelli.tripledger.shared.domain.enums.AttachmentFileType;
import com.spvermicelli.tripledger.shared.domain.enums.BillChangeFlowStatus;
import com.spvermicelli.tripledger.shared.domain.enums.ChangeRequestStatus;
import com.spvermicelli.tripledger.shared.domain.enums.ChangeRequestType;
import com.spvermicelli.tripledger.shared.domain.enums.OperationTargetType;
import com.spvermicelli.tripledger.shared.domain.enums.OperationType;
import com.spvermicelli.tripledger.shared.domain.enums.ShareMethod;
import com.spvermicelli.tripledger.shared.domain.enums.ShareParticipantType;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
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
 * 账单申请应用服务。
 * 使用“申请主表 + 快照表 + 快照分摊 + 快照附件”保存全过程，可完整回放 A->B->C 的申请链路。
 */
@Service
public class BillRequestApplicationService {

    private final BillingAccessSupportService billingAccessSupportService;
    private final BillRepository billRepository;
    private final BillShareItemRepository billShareItemRepository;
    private final BillAttachmentRepository billAttachmentRepository;
    private final BillChangeRequestRepository billChangeRequestRepository;
    private final BillChangeRequestApprovalRepository billChangeRequestApprovalRepository;
    private final BillChangeRequestSnapshotRepository billChangeRequestSnapshotRepository;
    private final BillChangeRequestShareItemRepository billChangeRequestShareItemRepository;
    private final BillChangeRequestAttachmentRepository billChangeRequestAttachmentRepository;
    private final BookMemberRepository bookMemberRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final TempParticipantRepository tempParticipantRepository;
    private final BookUserDirectory bookUserDirectory;
    private final BillApplicationService billApplicationService;
    private final OperationLogService operationLogService;

    public BillRequestApplicationService(
        BillingAccessSupportService billingAccessSupportService,
        BillRepository billRepository,
        BillShareItemRepository billShareItemRepository,
        BillAttachmentRepository billAttachmentRepository,
        BillChangeRequestRepository billChangeRequestRepository,
        BillChangeRequestApprovalRepository billChangeRequestApprovalRepository,
        BillChangeRequestSnapshotRepository billChangeRequestSnapshotRepository,
        BillChangeRequestShareItemRepository billChangeRequestShareItemRepository,
        BillChangeRequestAttachmentRepository billChangeRequestAttachmentRepository,
        BookMemberRepository bookMemberRepository,
        BookCategoryRepository bookCategoryRepository,
        TempParticipantRepository tempParticipantRepository,
        BookUserDirectory bookUserDirectory,
        BillApplicationService billApplicationService,
        OperationLogService operationLogService
    ) {
        this.billingAccessSupportService = billingAccessSupportService;
        this.billRepository = billRepository;
        this.billShareItemRepository = billShareItemRepository;
        this.billAttachmentRepository = billAttachmentRepository;
        this.billChangeRequestRepository = billChangeRequestRepository;
        this.billChangeRequestApprovalRepository = billChangeRequestApprovalRepository;
        this.billChangeRequestSnapshotRepository = billChangeRequestSnapshotRepository;
        this.billChangeRequestShareItemRepository = billChangeRequestShareItemRepository;
        this.billChangeRequestAttachmentRepository = billChangeRequestAttachmentRepository;
        this.bookMemberRepository = bookMemberRepository;
        this.bookCategoryRepository = bookCategoryRepository;
        this.tempParticipantRepository = tempParticipantRepository;
        this.bookUserDirectory = bookUserDirectory;
        this.billApplicationService = billApplicationService;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public BillRequestOperationResult createModifyRequest(CreateModifyBillRequestCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        validateModifyRequestCommand(command);

        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(command.getCurrentUserId(), command.getBookId());
        VisibleBillAggregate visibleBill = billingAccessSupportService.requireVisibleBill(
            command.getBookId(),
            command.getBillId(),
            command.getCurrentUserId()
        );
        if (canDirectOperate(context.getCurrentMember(), visibleBill)) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前账单已具备直接修改权限，无需发起修改申请");
        }

        Bill currentBill = visibleBill.getBill();
        ensureBaselineSnapshot(currentBill, context.getCurrentMember().getId());
        obsoletePendingRequest(command.getBookId(), command.getBillId());

        BillChangeRequest latest = loadLatestRequest(command.getBillId());
        LocalDateTime now = LocalDateTime.now();
        BillChangeRequest savedRequest = billChangeRequestRepository.save(BillChangeRequest.builder()
            .billId(command.getBillId())
            .predecessorRequestId(latest == null ? null : latest.getId())
            .baseline(false)
            .requestType(ChangeRequestType.MODIFY)
            .requesterMemberId(context.getCurrentMember().getId())
            .requestReason(normalizeText(command.getRequestReason()))
            .status(ChangeRequestStatus.PENDING)
            .createdAt(now)
            .build());

        saveCommandSnapshot(savedRequest, command, context.getCurrentMember().getId());
        refreshBillChangeFlowState(command.getBillId());
        operationLogService.log(
            command.getBookId(),
            context.getCurrentMember().getUserId(),
            context.getCurrentMember().getId(),
            OperationType.CREATE_CHANGE_REQUEST,
            OperationTargetType.BILL_CHANGE_REQUEST,
            savedRequest.getId(),
            null,
            "type=MODIFY,status=PENDING"
        );

        return BillRequestOperationResult.builder()
            .requestId(savedRequest.getId())
            .message("账单修改申请已发起，等待审批")
            .build();
    }

    @Transactional
    public BillRequestOperationResult createDeleteRequest(CreateDeleteBillRequestCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        if (command.getBillId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "billId 不能为空");
        }

        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(command.getCurrentUserId(), command.getBookId());
        VisibleBillAggregate visibleBill = billingAccessSupportService.requireVisibleBill(
            command.getBookId(),
            command.getBillId(),
            command.getCurrentUserId()
        );
        if (canDirectOperate(context.getCurrentMember(), visibleBill)) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前账单已具备直接删除权限，无需发起删除申请");
        }

        Bill currentBill = visibleBill.getBill();
        ensureBaselineSnapshot(currentBill, context.getCurrentMember().getId());
        obsoletePendingRequest(command.getBookId(), command.getBillId());

        BillChangeRequest latest = loadLatestRequest(command.getBillId());
        LocalDateTime now = LocalDateTime.now();
        BillChangeRequest savedRequest = billChangeRequestRepository.save(BillChangeRequest.builder()
            .billId(command.getBillId())
            .predecessorRequestId(latest == null ? null : latest.getId())
            .baseline(false)
            .requestType(ChangeRequestType.DELETE)
            .requesterMemberId(context.getCurrentMember().getId())
            .requestReason(normalizeText(command.getRequestReason()))
            .status(ChangeRequestStatus.PENDING)
            .createdAt(now)
            .build());

        saveBillSnapshot(
            savedRequest,
            currentBill,
            visibleBill.getShareItems(),
            billAttachmentRepository.findByBillId(currentBill.getId()).stream().map(BillAttachment::getFileUrl).toList(),
            context.getCurrentMember().getId()
        );
        refreshBillChangeFlowState(command.getBillId());
        operationLogService.log(
            command.getBookId(),
            context.getCurrentMember().getUserId(),
            context.getCurrentMember().getId(),
            OperationType.CREATE_CHANGE_REQUEST,
            OperationTargetType.BILL_CHANGE_REQUEST,
            savedRequest.getId(),
            null,
            "type=DELETE,status=PENDING"
        );

        return BillRequestOperationResult.builder()
            .requestId(savedRequest.getId())
            .message("账单删除申请已发起，等待审批")
            .build();
    }

    @Transactional(readOnly = true)
    public List<BillRequestDetailResult> getMyRequests(Long currentUserId, Long bookId) {
        validateCurrentUser(currentUserId);
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(currentUserId, bookId);
        return billChangeRequestRepository.findByBookIdAndRequesterMemberId(bookId, context.getCurrentMember().getId())
            .stream()
            .map(request -> toDetailResult(bookId, request, context.getCurrentMember().getId()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<BillRequestDetailResult> getPendingApproveRequests(Long currentUserId, Long bookId) {
        validateCurrentUser(currentUserId);
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(currentUserId, bookId);
        return billChangeRequestRepository.findByBookIdAndStatus(bookId, ChangeRequestStatus.PENDING)
            .stream()
            .filter(request -> resolveApproverMemberIds(bookId, request.getBillId()).contains(context.getCurrentMember().getId()))
            .map(request -> toDetailResult(bookId, request, context.getCurrentMember().getId()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<BillRequestDetailResult> getBillRequests(Long currentUserId, Long bookId, Long billId) {
        validateCurrentUser(currentUserId);
        if (billId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "billId 不能为空");
        }
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(currentUserId, bookId);
        billingAccessSupportService.requireVisibleBill(bookId, billId, currentUserId);
        return billChangeRequestRepository.findByBillId(billId).stream()
            .map(request -> toDetailResult(bookId, request, context.getCurrentMember().getId()))
            .toList();
    }

    @Transactional(readOnly = true)
    public BillRequestDetailResult getRequestDetail(Long currentUserId, Long bookId, Long requestId) {
        validateCurrentUser(currentUserId);
        if (requestId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "requestId 不能为空");
        }
        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(currentUserId, bookId);
        BillChangeRequest request = billChangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "申请不存在"));
        Bill bill = billRepository.findById(request.getBillId())
            .filter(item -> Objects.equals(item.getBookId(), bookId))
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "申请对应账单不存在"));
        billingAccessSupportService.requireVisibleBill(bookId, bill.getId(), currentUserId);
        return toDetailResult(bookId, request, context.getCurrentMember().getId());
    }

    @Transactional
    public BillRequestOperationResult cancelRequest(CancelBillRequestCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        if (command.getRequestId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "requestId 不能为空");
        }

        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(command.getCurrentUserId(), command.getBookId());
        BillChangeRequest request = billChangeRequestRepository.findById(command.getRequestId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "申请不存在"));
        Bill bill = billRepository.findById(request.getBillId())
            .filter(item -> Objects.equals(item.getBookId(), command.getBookId()))
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "申请对应的账单不存在"));
        billingAccessSupportService.requireVisibleBill(command.getBookId(), bill.getId(), command.getCurrentUserId());

        if (!request.isPending()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "当前申请已处理，无法撤销");
        }
        if (!Objects.equals(request.getRequesterMemberId(), context.getCurrentMember().getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅发起人可撤销该申请");
        }
        billChangeRequestRepository.save(request.cancel(LocalDateTime.now()));
        refreshBillChangeFlowState(request.getBillId());
        operationLogService.log(
            command.getBookId(),
            context.getCurrentMember().getUserId(),
            context.getCurrentMember().getId(),
            OperationType.CANCEL_CHANGE_REQUEST,
            OperationTargetType.BILL_CHANGE_REQUEST,
            request.getId(),
            "status=PENDING",
            "status=CANCELLED"
        );
        return BillRequestOperationResult.builder()
            .requestId(request.getId())
            .message("申请已撤销")
            .build();
    }

    @Transactional
    public BillRequestOperationResult approveRequest(ApproveBillRequestCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        if (command.getRequestId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "requestId 不能为空");
        }

        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(command.getCurrentUserId(), command.getBookId());
        BillChangeRequest request = requirePendingRequest(command.getBookId(), command.getRequestId());
        ensureApprover(command.getBookId(), request, context.getCurrentMember());

        billChangeRequestApprovalRepository.save(BillChangeRequestApproval.builder()
            .requestId(request.getId())
            .approverMemberId(context.getCurrentMember().getId())
            .approvalAction(ApprovalAction.APPROVE)
            .approvalComment(normalizeText(command.getApprovalComment()))
            .createdAt(LocalDateTime.now())
            .build());

        if (request.getRequestType() == ChangeRequestType.MODIFY) {
            applyModifyRequest(command.getCurrentUserId(), command.getBookId(), request);
        } else {
            applyDeleteRequest(command.getCurrentUserId(), command.getBookId(), request);
        }
        billChangeRequestRepository.save(request.approve(LocalDateTime.now()));
        refreshBillChangeFlowState(request.getBillId());
        operationLogService.log(
            command.getBookId(),
            context.getCurrentMember().getUserId(),
            context.getCurrentMember().getId(),
            OperationType.APPROVE_CHANGE_REQUEST,
            OperationTargetType.BILL_CHANGE_REQUEST,
            request.getId(),
            "status=PENDING",
            "status=APPROVED"
        );
        return BillRequestOperationResult.builder()
            .requestId(request.getId())
            .message("申请已审批通过并完成执行")
            .build();
    }

    @Transactional
    public BillRequestOperationResult rejectRequest(RejectBillRequestCommand command) {
        validateCurrentUser(command.getCurrentUserId());
        if (command.getRequestId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "requestId 不能为空");
        }

        BillingBookMemberContext context = billingAccessSupportService.requireActiveContext(command.getCurrentUserId(), command.getBookId());
        BillChangeRequest request = requirePendingRequest(command.getBookId(), command.getRequestId());
        ensureApprover(command.getBookId(), request, context.getCurrentMember());

        billChangeRequestApprovalRepository.save(BillChangeRequestApproval.builder()
            .requestId(request.getId())
            .approverMemberId(context.getCurrentMember().getId())
            .approvalAction(ApprovalAction.REJECT)
            .approvalComment(normalizeText(command.getApprovalComment()))
            .createdAt(LocalDateTime.now())
            .build());
        billChangeRequestRepository.save(request.reject(LocalDateTime.now()));
        refreshBillChangeFlowState(request.getBillId());
        operationLogService.log(
            command.getBookId(),
            context.getCurrentMember().getUserId(),
            context.getCurrentMember().getId(),
            OperationType.REJECT_CHANGE_REQUEST,
            OperationTargetType.BILL_CHANGE_REQUEST,
            request.getId(),
            "status=PENDING",
            "status=REJECTED"
        );

        return BillRequestOperationResult.builder()
            .requestId(request.getId())
            .message("申请已拒绝")
            .build();
    }

    private void applyModifyRequest(Long currentUserId, Long bookId, BillChangeRequest request) {
        BillChangeRequestSnapshot snapshot = billChangeRequestSnapshotRepository.findByRequestId(request.getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "申请快照不存在"));

        List<BillShareItemCommand> shareItemCommands = billChangeRequestShareItemRepository.findByRequestId(request.getId()).stream()
            .map(item -> BillShareItemCommand.builder()
                .participantType(item.getParticipantType().getCode())
                .participantRefId(item.getParticipantRefId())
                .shareMethod(item.getShareMethod().getCode())
                .shareRatio(item.getShareRatio())
                .shareAmountCent(item.getShareAmountCent())
                .build())
            .toList();

        List<String> attachmentUrls = billChangeRequestAttachmentRepository.findByRequestId(request.getId()).stream()
            .map(BillChangeRequestAttachment::getFileUrl)
            .toList();

        billApplicationService.updateBill(UpdateBillCommand.builder()
            .currentUserId(currentUserId)
            .bookId(bookId)
            .billId(request.getBillId())
            .fromChangeRequest(true)
            .billType(snapshot.getBillType())
            .title(snapshot.getTitle())
            .billAmountCent(snapshot.getBillAmountCent())
            .categoryId(snapshot.getCategoryId())
            .payerMemberId(snapshot.getPayerMemberId())
            .recorderMemberId(snapshot.getRecorderMemberId())
            .tempParticipantId(snapshot.getTempParticipantId())
            .billTime(snapshot.getBillTime())
            .remark(snapshot.getRemark())
            .attachmentUrls(attachmentUrls)
            .shareItems(shareItemCommands)
            .build());
    }

    private void applyDeleteRequest(Long currentUserId, Long bookId, BillChangeRequest request) {
        billApplicationService.deleteBill(DeleteBillCommand.builder()
            .currentUserId(currentUserId)
            .bookId(bookId)
            .billId(request.getBillId())
            .fromChangeRequest(true)
            .build());
    }

    private BillRequestDetailResult toDetailResult(Long bookId, BillChangeRequest request, Long currentMemberId) {
        List<BillChangeRequestApproval> approvals = billChangeRequestApprovalRepository.findByRequestId(request.getId());
        Set<Long> approverIds = resolveApproverMemberIds(bookId, request.getBillId());
        return BillRequestDetailResult.builder()
            .requestId(request.getId())
            .billId(request.getBillId())
            .predecessorRequestId(request.getPredecessorRequestId())
            .baseline(request.isBaseline())
            .requestType(request.getRequestType().getCode())
            .requesterMemberId(request.getRequesterMemberId())
            .requestReason(request.getRequestReason())
            .status(request.getStatus().getCode())
            .createdAt(request.getCreatedAt())
            .handledAt(request.getHandledAt())
            .currentUserCanApprove(approverIds.contains(currentMemberId) && request.isPending())
            .currentUserCanCancel(Objects.equals(request.getRequesterMemberId(), currentMemberId) && request.isPending())
            .snapshot(loadSnapshotResult(bookId, request.getId()))
            .approvalRecords(approvals.stream()
                .map(approval -> BillRequestDetailResult.ApprovalRecordResult.builder()
                    .approvalId(approval.getId())
                    .approverMemberId(approval.getApproverMemberId())
                    .approvalAction(approval.getApprovalAction().getCode())
                    .approvalComment(approval.getApprovalComment())
                    .createdAt(approval.getCreatedAt())
                    .build())
                .toList())
            .build();
    }

    private BillRequestDetailResult.SnapshotResult loadSnapshotResult(Long bookId, Long requestId) {
        BillChangeRequestSnapshot snapshot = billChangeRequestSnapshotRepository.findByRequestId(requestId).orElse(null);
        if (snapshot == null) {
            return null;
        }

        List<BillChangeRequestShareItem> shareItems = billChangeRequestShareItemRepository.findByRequestId(requestId);
        List<String> attachmentUrls = billChangeRequestAttachmentRepository.findByRequestId(requestId).stream()
            .map(BillChangeRequestAttachment::getFileUrl)
            .toList();

        Set<Long> memberIds = new java.util.HashSet<>();
        memberIds.add(snapshot.getPayerMemberId());
        memberIds.add(snapshot.getRecorderMemberId());
        shareItems.forEach(item -> {
            if (item.getParticipantType() == ShareParticipantType.MEMBER) {
                memberIds.add(item.getParticipantRefId());
            }
            if (item.getAttachedMemberId() != null) {
                memberIds.add(item.getAttachedMemberId());
            }
        });

        Map<Long, BookMember> memberMap = memberIds.stream()
            .filter(Objects::nonNull)
            .map(bookMemberRepository::findById)
            .flatMap(java.util.Optional::stream)
            .filter(member -> Objects.equals(member.getBookId(), bookId))
            .collect(Collectors.toMap(BookMember::getId, Function.identity(), (left, right) -> left));
        Map<Long, BookUserSummary> userSummaryMap = bookUserDirectory.getByUserIds(memberMap.values().stream()
            .map(BookMember::getUserId)
            .distinct()
            .toList());

        BookCategory category = snapshot.getCategoryId() == null
            ? null
            : bookCategoryRepository.findById(snapshot.getCategoryId()).orElse(null);
        TempParticipant tempParticipant = snapshot.getTempParticipantId() == null
            ? null
            : tempParticipantRepository.findById(snapshot.getTempParticipantId()).orElse(null);

        return BillRequestDetailResult.SnapshotResult.builder()
            .billType(snapshot.getBillType())
            .title(snapshot.getTitle())
            .billAmountCent(snapshot.getBillAmountCent())
            .categoryId(snapshot.getCategoryId())
            .categoryName(category == null ? null : category.getName())
            .categoryIcon(category == null ? null : category.getIcon())
            .payerMemberId(snapshot.getPayerMemberId())
            .payerMemberName(memberName(snapshot.getPayerMemberId(), memberMap, userSummaryMap))
            .recorderMemberId(snapshot.getRecorderMemberId())
            .recorderMemberName(memberName(snapshot.getRecorderMemberId(), memberMap, userSummaryMap))
            .tempParticipantId(snapshot.getTempParticipantId())
            .tempParticipantNickname(tempParticipant == null ? null : tempParticipant.getNickname())
            .billTime(snapshot.getBillTime())
            .remark(snapshot.getRemark())
            .attachmentUrls(attachmentUrls)
            .shareItems(shareItems.stream()
                .map(item -> BillRequestDetailResult.SnapshotShareItemResult.builder()
                    .participantType(item.getParticipantType().getCode())
                    .participantRefId(item.getParticipantRefId())
                    .participantName(resolveShareParticipantName(item, memberMap, userSummaryMap))
                    .attachedMemberId(item.getAttachedMemberId())
                    .attachedMemberName(memberName(item.getAttachedMemberId(), memberMap, userSummaryMap))
                    .shareMethod(item.getShareMethod().getCode())
                    .shareRatio(item.getShareRatio())
                    .shareAmountCent(item.getShareAmountCent())
                    .build())
                .toList())
            .build();
    }

    private String resolveShareParticipantName(
        BillChangeRequestShareItem item,
        Map<Long, BookMember> memberMap,
        Map<Long, BookUserSummary> userSummaryMap
    ) {
        if (item.getParticipantType() == ShareParticipantType.MEMBER) {
            return memberName(item.getParticipantRefId(), memberMap, userSummaryMap);
        }
        return tempParticipantRepository.findById(item.getParticipantRefId())
            .map(TempParticipant::getNickname)
            .orElse("临时成员#" + item.getParticipantRefId());
    }

    private String memberName(
        Long memberId,
        Map<Long, BookMember> memberMap,
        Map<Long, BookUserSummary> userSummaryMap
    ) {
        if (memberId == null) {
            return null;
        }
        BookMember member = memberMap.get(memberId);
        if (member == null) {
            return "成员#" + memberId;
        }
        BookUserSummary summary = userSummaryMap.get(member.getUserId());
        return summary == null ? ("成员#" + memberId) : summary.getNickname();
    }

    private BillChangeRequest requirePendingRequest(Long bookId, Long requestId) {
        BillChangeRequest request = billChangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "申请不存在"));
        Bill bill = billRepository.findById(request.getBillId())
            .filter(item -> Objects.equals(item.getBookId(), bookId))
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "申请对应的账单不存在"));
        if (!bill.isActive()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "申请对应账单已失效");
        }
        if (request.getStatus() == ChangeRequestStatus.OUTDATED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "该申请已过期，请查看最新内容");
        }
        if (bill.getLatestChangeRequestId() != null && !Objects.equals(bill.getLatestChangeRequestId(), request.getId())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "该申请已过期，请查看最新内容");
        }
        if (!request.isPending()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "当前申请已处理，无法重复操作");
        }
        return request;
    }

    private void ensureApprover(Long bookId, BillChangeRequest request, BookMember currentMember) {
        if (!resolveApproverMemberIds(bookId, request.getBillId()).contains(currentMember.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无权审批该申请");
        }
    }

    private Set<Long> resolveApproverMemberIds(Long bookId, Long billId) {
        Bill bill = billRepository.findById(billId)
            .filter(item -> Objects.equals(item.getBookId(), bookId))
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "账单不存在"));
        List<BillShareItem> shareItems = bill.isSharedExpense()
            ? billShareItemRepository.findByBillId(billId)
            : List.of();
        Set<Long> approverIds = new LinkedHashSet<>();
        approverIds.add(bill.getPayerMemberId());

        BookMember recorderMember = bookMemberRepository.findById(bill.getRecorderMemberId())
            .orElse(null);
        if (recorderMember != null
            && recorderMember.isActive()
            && Objects.equals(recorderMember.getBookId(), bookId)
            && billingAccessSupportService.canViewBill(recorderMember, bill, shareItems)) {
            approverIds.add(recorderMember.getId());
        }
        return approverIds;
    }

    private boolean canDirectOperate(BookMember currentMember, VisibleBillAggregate visibleBill) {
        return Objects.equals(currentMember.getId(), visibleBill.getBill().getPayerMemberId())
            || (Objects.equals(currentMember.getId(), visibleBill.getBill().getRecorderMemberId())
                && billingAccessSupportService.canViewBill(currentMember, visibleBill.getBill(), visibleBill.getShareItems()));
    }

    private void obsoletePendingRequest(Long bookId, Long billId) {
        LocalDateTime now = LocalDateTime.now();
        billChangeRequestRepository.findByBookIdAndStatus(bookId, ChangeRequestStatus.PENDING).stream()
            .filter(request -> Objects.equals(request.getBillId(), billId))
            .forEach(request -> billChangeRequestRepository.save(request.obsolete(now)));
    }

    private void ensureBaselineSnapshot(Bill bill, Long requesterMemberId) {
        if (billChangeRequestRepository.findByBillId(bill.getId()).stream().anyMatch(BillChangeRequest::isBaseline)) {
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

        saveBillSnapshot(
            baseline,
            bill,
            bill.isSharedExpense() ? billShareItemRepository.findByBillId(bill.getId()) : List.of(),
            billAttachmentRepository.findByBillId(bill.getId()).stream().map(BillAttachment::getFileUrl).toList(),
            requesterMemberId
        );
        refreshBillChangeFlowState(bill.getId());
    }

    private void saveCommandSnapshot(BillChangeRequest request, CreateModifyBillRequestCommand command, Long uploadedByMemberId) {
        billChangeRequestSnapshotRepository.save(BillChangeRequestSnapshot.builder()
            .requestId(request.getId())
            .billType(command.getBillType())
            .title(command.getTitle())
            .billAmountCent(command.getBillAmountCent())
            .categoryId(command.getCategoryId())
            .payerMemberId(command.getPayerMemberId())
            .recorderMemberId(command.getRecorderMemberId())
            .tempParticipantId(command.getTempParticipantId())
            .billTime(command.getBillTime())
            .remark(normalizeText(command.getRemark()))
            .build());

        List<BillChangeRequestShareItem> shareItems = command.getShareItems() == null
            ? List.of()
            : command.getShareItems().stream()
                .map(item -> BillChangeRequestShareItem.builder()
                    .requestId(request.getId())
                    .participantType(parseShareParticipantType(item.getParticipantType()))
                    .participantRefId(item.getParticipantRefId())
                    .attachedMemberId(null)
                    .shareMethod(parseShareMethod(item.getShareMethod()))
                    .shareRatio(item.getShareRatio())
                    .shareAmountCent(item.getShareAmountCent())
                    .build())
                .toList();
        billChangeRequestShareItemRepository.saveAll(request.getId(), shareItems);
        billChangeRequestAttachmentRepository.saveAll(request.getId(), normalizeAttachmentUrls(command.getAttachmentUrls()).stream()
            .map(url -> BillChangeRequestAttachment.builder()
                .requestId(request.getId())
                .fileUrl(url)
                .fileType(resolveAttachmentFileType(url))
                .uploadedByMemberId(uploadedByMemberId)
                .build())
            .toList());
    }

    private void saveBillSnapshot(
        BillChangeRequest request,
        Bill snapshotBill,
        List<BillShareItem> shareItems,
        List<String> attachmentUrls,
        Long uploadedByMemberId
    ) {
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

        billChangeRequestShareItemRepository.saveAll(request.getId(), shareItems.stream()
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
                .fileType(resolveAttachmentFileType(url))
                .uploadedByMemberId(uploadedByMemberId)
                .build())
            .toList());
    }

    private BillChangeRequest loadLatestRequest(Long billId) {
        List<BillChangeRequest> requests = billChangeRequestRepository.findByBillId(billId);
        return requests.isEmpty() ? null : requests.getFirst();
    }

    private void refreshBillChangeFlowState(Long billId) {
        Bill bill = billRepository.findById(billId).orElse(null);
        if (bill == null) {
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
            .id(bill.getId())
            .bookId(bill.getBookId())
            .billType(bill.getBillType())
            .title(bill.getTitle())
            .billAmountCent(bill.getBillAmountCent())
            .categoryId(bill.getCategoryId())
            .payerMemberId(bill.getPayerMemberId())
            .recorderMemberId(bill.getRecorderMemberId())
            .targetTempParticipantId(bill.getTargetTempParticipantId())
            .billTime(bill.getBillTime())
            .remark(bill.getRemark())
            .changeFlowStatus(flowStatus)
            .latestChangeRequestId(latest == null ? null : latest.getId())
            .hasChangeHistory(!requests.isEmpty())
            .status(bill.getStatus())
            .createdAt(bill.getCreatedAt())
            .updatedAt(bill.getUpdatedAt())
            .build());
    }

    private void validateModifyRequestCommand(CreateModifyBillRequestCommand command) {
        if (command.getBillId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "billId 不能为空");
        }
        if (!StringUtils.hasText(command.getBillType())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "billType 不能为空");
        }
        if (!StringUtils.hasText(command.getTitle())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "title 不能为空");
        }
        if (command.getBillAmountCent() == null || command.getBillAmountCent() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "billAmountCent 必须大于 0");
        }
        if (command.getCategoryId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "categoryId 不能为空");
        }
        if (command.getPayerMemberId() == null || command.getRecorderMemberId() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "payerMemberId、recorderMemberId 不能为空");
        }
        if (command.getBillTime() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "billTime 不能为空");
        }
    }

    private void validateCurrentUser(Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前登录信息不存在");
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

    private AttachmentFileType resolveAttachmentFileType(String url) {
        String lower = Objects.toString(url, "").toLowerCase();
        if (lower.endsWith(".jpg")
            || lower.endsWith(".jpeg")
            || lower.endsWith(".png")
            || lower.endsWith(".gif")
            || lower.endsWith(".webp")
            || lower.contains("image")) {
            return AttachmentFileType.IMAGE;
        }
        return AttachmentFileType.OTHER;
    }

    private String normalizeText(String rawText) {
        return StringUtils.hasText(rawText) ? rawText.trim() : null;
    }
}
