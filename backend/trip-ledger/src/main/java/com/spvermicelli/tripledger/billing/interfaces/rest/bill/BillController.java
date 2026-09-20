package com.spvermicelli.tripledger.billing.interfaces.rest.bill;

import com.spvermicelli.tripledger.billing.application.bill.BillApplicationService;
import com.spvermicelli.tripledger.billing.application.bill.command.BillShareItemCommand;
import com.spvermicelli.tripledger.billing.application.bill.command.CreatePersonalBillCommand;
import com.spvermicelli.tripledger.billing.application.bill.command.CreateSharedExpenseBillCommand;
import com.spvermicelli.tripledger.billing.application.bill.command.UpdateBillCommand;
import com.spvermicelli.tripledger.billing.application.bill.result.BillDetailResult;
import com.spvermicelli.tripledger.billing.application.bill.result.BillListItemResult;
import com.spvermicelli.tripledger.billing.application.bill.result.BillOperationResult;
import com.spvermicelli.tripledger.billing.interfaces.rest.bill.request.CreatePersonalBillRequest;
import com.spvermicelli.tripledger.billing.interfaces.rest.bill.request.CreateSharedExpenseBillRequest;
import com.spvermicelli.tripledger.billing.interfaces.rest.bill.request.SettleBillParticipantRequest;
import com.spvermicelli.tripledger.billing.interfaces.rest.bill.request.UpdateBillRequest;
import com.spvermicelli.tripledger.billing.interfaces.rest.bill.response.BillDetailResponse;
import com.spvermicelli.tripledger.billing.interfaces.rest.bill.response.BillListItemResponse;
import com.spvermicelli.tripledger.billing.interfaces.rest.bill.response.BillOperationResponse;
import com.spvermicelli.tripledger.shared.common.context.UserContextHolder;
import com.spvermicelli.tripledger.shared.common.response.ApiResponse;
import com.spvermicelli.tripledger.shared.common.response.PageResponse;
import com.spvermicelli.tripledger.shared.infrastructure.redis.RedisDistributedLockService;
import com.spvermicelli.tripledger.shared.infrastructure.redis.RedisIdempotencyService;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.DurableIdempotencyService;
import com.spvermicelli.tripledger.billing.application.support.BillingAccessSupportService;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.function.Supplier;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账单接口。
 * Controller 只做 HTTP 协议适配，不做任何权限或金额计算判断。
 */
@RestController
@RequestMapping("/api/v1/books/{bookId}/bills")
public class BillController {

    private final DurableIdempotencyService durableIdempotency;
    private final BillingAccessSupportService access;
    private final BillApplicationService billApplicationService;
    private final RedisDistributedLockService redisDistributedLockService;
    private final RedisIdempotencyService redisIdempotencyService;

    public BillController(
        BillApplicationService billApplicationService,
        RedisDistributedLockService redisDistributedLockService,
        RedisIdempotencyService redisIdempotencyService,
        DurableIdempotencyService durableIdempotency,
        BillingAccessSupportService access
    ) {
        this.durableIdempotency = durableIdempotency;
        this.access = access;
        this.billApplicationService = billApplicationService;
        this.redisDistributedLockService = redisDistributedLockService;
        this.redisIdempotencyService = redisIdempotencyService;
    }

    @GetMapping
    public ApiResponse<PageResponse<BillListItemResponse>> getVisibleBills(
        @PathVariable Long bookId,
        @RequestParam(required = false) Integer pageNo,
        @RequestParam(required = false) Integer pageSize
    ) {
        PageResponse<BillListItemResult> resultPage = billApplicationService.getVisibleBills(
            UserContextHolder.getUserId(),
            bookId,
            pageNo,
            pageSize
        );
        return ApiResponse.success(PageResponse.<BillListItemResponse>builder()
            .list(resultPage.getList().stream().map(this::toBillListItemResponse).toList())
            .pageNum(resultPage.getPageNum())
            .pageSize(resultPage.getPageSize())
            .total(resultPage.getTotal())
            .build());
    }

    @GetMapping("/{billId}")
    public ApiResponse<BillDetailResponse> getBillDetail(@PathVariable Long bookId, @PathVariable Long billId) {
        return ApiResponse.success(toBillDetailResponse(
            billApplicationService.getBillDetail(UserContextHolder.getUserId(), bookId, billId)
        ));
    }

    @PostMapping("/personal-bill")
    public ApiResponse<BillOperationResponse> createPersonalBill(
        @PathVariable Long bookId,
        @Valid @RequestBody CreatePersonalBillRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String requestKey
    ) {
        Long currentUserId = UserContextHolder.getUserId();
        Supplier<BillOperationResult> action = () -> billApplicationService.createPersonalBill(CreatePersonalBillCommand.builder()
                    .currentUserId(currentUserId)
                    .bookId(bookId)
                    .billType(request.getBillType())
                    .title(request.getTitle())
                    .billAmountCent(request.getBillAmountCent())
                    .categoryId(request.getCategoryId())
                    .payerMemberId(request.getPayerMemberId())
                    .recorderMemberId(request.getRecorderMemberId())
                    .tempParticipantId(request.getTempParticipantId())
                    .billTime(request.getBillTime())
                    .remark(request.getRemark())
                    .attachmentUrls(request.getAttachmentUrls())
                    .build());
        BillOperationResult result = redisDistributedLockService.executeWithBookLock(bookId, () -> {
            if (requestKey != null) {
                return durableIdempotency.execute(currentUserId, bookId, "bill:create-personal", requestKey,
                    request, BillOperationResult.class,
                    () -> access.requireActiveContext(currentUserId, bookId), action);
            }
            return redisIdempotencyService.executeOnce(
                "bill:create-personal:" + currentUserId + ":" + bookId + ":" + request.getTitle() + ":" + request.getBillAmountCent() + ":" + request.getBillTime(), Duration.ofSeconds(30), action);
        });
        return ApiResponse.success(toBillOperationResponse(result));
    }

    @PostMapping("/shared-expense")
    public ApiResponse<BillOperationResponse> createSharedExpenseBill(
        @PathVariable Long bookId,
        @Valid @RequestBody CreateSharedExpenseBillRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String requestKey
    ) {
        Long currentUserId = UserContextHolder.getUserId();
        Supplier<BillOperationResult> action = () -> billApplicationService.createSharedExpenseBill(CreateSharedExpenseBillCommand.builder()
                    .currentUserId(currentUserId)
                    .bookId(bookId)
                    .title(request.getTitle())
                    .billAmountCent(request.getBillAmountCent())
                    .categoryId(request.getCategoryId())
                    .payerMemberId(request.getPayerMemberId())
                    .recorderMemberId(request.getRecorderMemberId())
                    .billTime(request.getBillTime())
                    .remark(request.getRemark())
                    .attachmentUrls(request.getAttachmentUrls())
                    .shareItems(toShareItemCommands(request.getShareItems()))
                    .build());
        BillOperationResult result = redisDistributedLockService.executeWithBookLock(bookId, () -> {
            if (requestKey != null) {
                return durableIdempotency.execute(currentUserId, bookId, "bill:create-shared", requestKey,
                    request, BillOperationResult.class,
                    () -> access.requireActiveContext(currentUserId, bookId), action);
            }
            return redisIdempotencyService.executeOnce(
                "bill:create-shared:" + currentUserId + ":" + bookId + ":" + request.getTitle() + ":" + request.getBillAmountCent() + ":" + request.getBillTime(), Duration.ofSeconds(30), action);
        });
        return ApiResponse.success(toBillOperationResponse(result));
    }

    @PutMapping("/{billId}")
    public ApiResponse<BillOperationResponse> updateBill(
        @PathVariable Long bookId,
        @PathVariable Long billId,
        @Valid @RequestBody UpdateBillRequest request
    ) {
        BillOperationResult result = redisDistributedLockService.executeWithBookLock(bookId, () ->
            billApplicationService.updateBill(UpdateBillCommand.builder()
                .currentUserId(UserContextHolder.getUserId())
                .bookId(bookId)
                .billId(billId)
                .fromChangeRequest(false)
                .billType(request.getBillType())
                .title(request.getTitle())
                .billAmountCent(request.getBillAmountCent())
                .categoryId(request.getCategoryId())
                .payerMemberId(request.getPayerMemberId())
                .recorderMemberId(request.getRecorderMemberId())
                .tempParticipantId(request.getTempParticipantId())
                .billTime(request.getBillTime())
                .remark(request.getRemark())
                .attachmentUrls(request.getAttachmentUrls())
                .shareItems(toUpdateShareItemCommands(request.getShareItems()))
                .build())
        );
        return ApiResponse.success(toBillOperationResponse(result));
    }

    @DeleteMapping("/{billId}")
    public ApiResponse<BillOperationResponse> deleteBill(@PathVariable Long bookId, @PathVariable Long billId) {
        BillOperationResult result = redisDistributedLockService.executeWithBookLock(bookId, () ->
            billApplicationService.deleteBill(
                com.spvermicelli.tripledger.billing.application.bill.command.DeleteBillCommand.builder()
                    .currentUserId(UserContextHolder.getUserId())
                    .bookId(bookId)
                    .billId(billId)
                    .fromChangeRequest(false)
                    .build()
            )
        );
        return ApiResponse.success(toBillOperationResponse(result));
    }

    @PostMapping("/{billId}/participants/{participantMemberId}/settle")
    public ApiResponse<BillOperationResponse> settleSharedBillParticipant(
        @PathVariable Long bookId,
        @PathVariable Long billId,
        @PathVariable Long participantMemberId,
        @Valid @RequestBody(required = false) SettleBillParticipantRequest request
    ) {
        BillOperationResult result = redisDistributedLockService.executeWithBookLock(bookId, () ->
            billApplicationService.settleSharedBillParticipant(
                UserContextHolder.getUserId(),
                bookId,
                billId,
                participantMemberId,
                request == null ? null : request.getRemark()
            )
        );
        return ApiResponse.success(toBillOperationResponse(result));
    }

    private List<BillShareItemCommand> toShareItemCommands(List<CreateSharedExpenseBillRequest.ShareItemRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream()
            .map(item -> BillShareItemCommand.builder()
                .participantType(item.getParticipantType())
                .participantRefId(item.getParticipantRefId())
                .shareMethod(item.getShareMethod())
                .shareRatio(item.getShareRatio())
                .shareAmountCent(item.getShareAmountCent())
                .build())
            .toList();
    }

    private List<BillShareItemCommand> toUpdateShareItemCommands(List<UpdateBillRequest.ShareItemRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream()
            .map(item -> BillShareItemCommand.builder()
                .participantType(item.getParticipantType())
                .participantRefId(item.getParticipantRefId())
                .shareMethod(item.getShareMethod())
                .shareRatio(item.getShareRatio())
                .shareAmountCent(item.getShareAmountCent())
                .build())
            .toList();
    }

    private BillListItemResponse toBillListItemResponse(BillListItemResult result) {
        return BillListItemResponse.builder()
            .billId(result.getBillId())
            .billType(result.getBillType())
            .title(result.getTitle())
            .billAmountCent(result.getBillAmountCent())
            .remark(result.getRemark())
            .categoryId(result.getCategoryId())
            .categoryName(result.getCategoryName())
            .categoryIcon(result.getCategoryIcon())
            .billTime(result.getBillTime())
            .createdAt(result.getCreatedAt())
            .viewerIsPayer(result.isViewerIsPayer())
            .viewerIsRecorder(result.isViewerIsRecorder())
            .viewerHasAttachedTempShare(result.isViewerHasAttachedTempShare())
            .directEditable(result.isDirectEditable())
            .directDeletable(result.isDirectDeletable())
            .viewerOwnShareAmountCent(result.getViewerOwnShareAmountCent())
            .viewerOwnPaidAmountCent(result.getViewerOwnPaidAmountCent())
            .viewerOwnUnpaidAmountCent(result.getViewerOwnUnpaidAmountCent())
            .viewerAttachedTempShareAmountCent(result.getViewerAttachedTempShareAmountCent())
            .viewerAttachedTempRecoveredAmountCent(result.getViewerAttachedTempRecoveredAmountCent())
            .viewerAttachedTempUnrecoveredAmountCent(result.getViewerAttachedTempUnrecoveredAmountCent())
            .viewerReceivableAmountCent(result.getViewerReceivableAmountCent())
            .viewerRecoveredAmountCent(result.getViewerRecoveredAmountCent())
            .viewerUnrecoveredAmountCent(result.getViewerUnrecoveredAmountCent())
            .build();
    }

    private BillDetailResponse toBillDetailResponse(BillDetailResult result) {
        return BillDetailResponse.builder()
            .billId(result.getBillId())
            .billType(result.getBillType())
            .title(result.getTitle())
            .billAmountCent(result.getBillAmountCent())
            .categoryId(result.getCategoryId())
            .categoryName(result.getCategoryName())
            .categoryIcon(result.getCategoryIcon())
            .billTime(result.getBillTime())
            .remark(result.getRemark())
            .attachmentUrls(result.getAttachmentUrls() == null ? List.of() : result.getAttachmentUrls())
            .changeFlowStatus(result.getChangeFlowStatus())
            .latestChangeRequestId(result.getLatestChangeRequestId())
            .hasChangeHistory(result.isHasChangeHistory())
            .payer(result.getPayer() == null ? null : BillDetailResponse.MemberSummaryResponse.builder()
                .memberId(result.getPayer().getMemberId())
                .userId(result.getPayer().getUserId())
                .nickname(result.getPayer().getNickname())
                .avatarUrl(result.getPayer().getAvatarUrl())
                .phoneNumber(result.getPayer().getPhoneNumber())
                .build())
            .recorder(result.getRecorder() == null ? null : BillDetailResponse.MemberSummaryResponse.builder()
                .memberId(result.getRecorder().getMemberId())
                .userId(result.getRecorder().getUserId())
                .nickname(result.getRecorder().getNickname())
                .avatarUrl(result.getRecorder().getAvatarUrl())
                .phoneNumber(result.getRecorder().getPhoneNumber())
                .build())
            .carryTargetTempParticipant(result.getCarryTargetTempParticipant() == null ? null : BillDetailResponse.TempSummaryResponse.builder()
                .tempParticipantId(result.getCarryTargetTempParticipant().getTempParticipantId())
                .nickname(result.getCarryTargetTempParticipant().getNickname())
                .attachedMemberId(result.getCarryTargetTempParticipant().getAttachedMemberId())
                .attachedMemberNickname(result.getCarryTargetTempParticipant().getAttachedMemberNickname())
                .build())
            .directEditable(result.isDirectEditable())
            .directDeletable(result.isDirectDeletable())
            .viewerReceivableAmountCent(result.getViewerReceivableAmountCent())
            .viewerRecoveredAmountCent(result.getViewerRecoveredAmountCent())
            .viewerUnrecoveredAmountCent(result.getViewerUnrecoveredAmountCent())
            .viewerOwnShareAmountCent(result.getViewerOwnShareAmountCent())
            .viewerOwnPaidAmountCent(result.getViewerOwnPaidAmountCent())
            .viewerOwnUnpaidAmountCent(result.getViewerOwnUnpaidAmountCent())
            .viewerAttachedTempShareAmountCent(result.getViewerAttachedTempShareAmountCent())
            .viewerAttachedTempRecoveredAmountCent(result.getViewerAttachedTempRecoveredAmountCent())
            .viewerAttachedTempUnrecoveredAmountCent(result.getViewerAttachedTempUnrecoveredAmountCent())
            .participants(result.getParticipants() == null ? List.of() : result.getParticipants().stream()
                .map(item -> BillDetailResponse.ParticipantResponse.builder()
                    .participantType(item.getParticipantType())
                    .participantRefId(item.getParticipantRefId())
                    .nickname(item.getNickname())
                    .avatarUrl(item.getAvatarUrl())
                    .attachedMemberId(item.getAttachedMemberId())
                    .attachedMemberNickname(item.getAttachedMemberNickname())
                    .shareAmountCent(item.getShareAmountCent())
                    .confirmedAmountCent(item.getConfirmedAmountCent())
                    .unconfirmedAmountCent(item.getUnconfirmedAmountCent())
                    .build())
                .toList())
            .createdAt(result.getCreatedAt())
            .updatedAt(result.getUpdatedAt())
            .build();
    }

    private BillOperationResponse toBillOperationResponse(BillOperationResult result) {
        return BillOperationResponse.builder()
            .billId(result.getBillId())
            .message(result.getMessage())
            .build();
    }
}
