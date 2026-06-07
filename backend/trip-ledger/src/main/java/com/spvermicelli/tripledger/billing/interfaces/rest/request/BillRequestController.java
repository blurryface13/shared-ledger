package com.spvermicelli.tripledger.billing.interfaces.rest.request;

import com.spvermicelli.tripledger.billing.application.bill.command.BillShareItemCommand;
import com.spvermicelli.tripledger.billing.application.request.BillRequestApplicationService;
import com.spvermicelli.tripledger.billing.application.request.command.ApproveBillRequestCommand;
import com.spvermicelli.tripledger.billing.application.request.command.CancelBillRequestCommand;
import com.spvermicelli.tripledger.billing.application.request.command.CreateDeleteBillRequestCommand;
import com.spvermicelli.tripledger.billing.application.request.command.CreateModifyBillRequestCommand;
import com.spvermicelli.tripledger.billing.application.request.command.RejectBillRequestCommand;
import com.spvermicelli.tripledger.billing.application.request.result.BillRequestDetailResult;
import com.spvermicelli.tripledger.billing.application.request.result.BillRequestOperationResult;
import com.spvermicelli.tripledger.billing.interfaces.rest.request.request.CreateDeleteBillRequestRequest;
import com.spvermicelli.tripledger.billing.interfaces.rest.request.request.CreateModifyBillRequestRequest;
import com.spvermicelli.tripledger.billing.interfaces.rest.request.request.HandleBillRequestRequest;
import com.spvermicelli.tripledger.billing.interfaces.rest.request.response.BillRequestDetailResponse;
import com.spvermicelli.tripledger.billing.interfaces.rest.request.response.BillRequestOperationResponse;
import com.spvermicelli.tripledger.shared.common.context.UserContextHolder;
import com.spvermicelli.tripledger.shared.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/books/{bookId}")
public class BillRequestController {

    private final BillRequestApplicationService billRequestApplicationService;

    public BillRequestController(BillRequestApplicationService billRequestApplicationService) {
        this.billRequestApplicationService = billRequestApplicationService;
    }

    @PostMapping("/bills/{billId}/modify-requests")
    public ApiResponse<BillRequestOperationResponse> createModifyRequest(
        @PathVariable Long bookId,
        @PathVariable Long billId,
        @Valid @RequestBody CreateModifyBillRequestRequest request
    ) {
        BillRequestOperationResult result = billRequestApplicationService.createModifyRequest(CreateModifyBillRequestCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .bookId(bookId)
            .billId(billId)
            .requestReason(request.getRequestReason())
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
            .shareItems(toShareItemCommands(request.getShareItems()))
            .build());
        return ApiResponse.success(toOperationResponse(result));
    }

    @PostMapping("/bills/{billId}/delete-requests")
    public ApiResponse<BillRequestOperationResponse> createDeleteRequest(
        @PathVariable Long bookId,
        @PathVariable Long billId,
        @Valid @RequestBody(required = false) CreateDeleteBillRequestRequest request
    ) {
        BillRequestOperationResult result = billRequestApplicationService.createDeleteRequest(CreateDeleteBillRequestCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .bookId(bookId)
            .billId(billId)
            .requestReason(request == null ? null : request.getRequestReason())
            .build());
        return ApiResponse.success(toOperationResponse(result));
    }

    @GetMapping("/bill-requests/my")
    public ApiResponse<List<BillRequestDetailResponse>> getMyRequests(@PathVariable Long bookId) {
        return ApiResponse.success(billRequestApplicationService.getMyRequests(UserContextHolder.getUserId(), bookId).stream()
            .map(this::toDetailResponse)
            .toList());
    }

    @GetMapping("/bill-requests/pending-approve")
    public ApiResponse<List<BillRequestDetailResponse>> getPendingApproveRequests(@PathVariable Long bookId) {
        return ApiResponse.success(
            billRequestApplicationService.getPendingApproveRequests(UserContextHolder.getUserId(), bookId).stream()
                .map(this::toDetailResponse)
                .toList()
        );
    }

    @GetMapping("/bills/{billId}/change-requests")
    public ApiResponse<List<BillRequestDetailResponse>> getBillRequests(@PathVariable Long bookId, @PathVariable Long billId) {
        return ApiResponse.success(
            billRequestApplicationService.getBillRequests(UserContextHolder.getUserId(), bookId, billId).stream()
                .map(this::toDetailResponse)
                .toList()
        );
    }

    @GetMapping("/bill-requests/{requestId}")
    public ApiResponse<BillRequestDetailResponse> getRequestDetail(@PathVariable Long bookId, @PathVariable Long requestId) {
        return ApiResponse.success(toDetailResponse(
            billRequestApplicationService.getRequestDetail(UserContextHolder.getUserId(), bookId, requestId)
        ));
    }

    @PostMapping("/bill-requests/{requestId}/approve")
    public ApiResponse<BillRequestOperationResponse> approveRequest(
        @PathVariable Long bookId,
        @PathVariable Long requestId,
        @Valid @RequestBody(required = false) HandleBillRequestRequest request
    ) {
        BillRequestOperationResult result = billRequestApplicationService.approveRequest(ApproveBillRequestCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .bookId(bookId)
            .requestId(requestId)
            .approvalComment(request == null ? null : request.getApprovalComment())
            .build());
        return ApiResponse.success(toOperationResponse(result));
    }

    @PostMapping("/bill-requests/{requestId}/reject")
    public ApiResponse<BillRequestOperationResponse> rejectRequest(
        @PathVariable Long bookId,
        @PathVariable Long requestId,
        @Valid @RequestBody(required = false) HandleBillRequestRequest request
    ) {
        BillRequestOperationResult result = billRequestApplicationService.rejectRequest(RejectBillRequestCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .bookId(bookId)
            .requestId(requestId)
            .approvalComment(request == null ? null : request.getApprovalComment())
            .build());
        return ApiResponse.success(toOperationResponse(result));
    }

    @PostMapping("/bill-requests/{requestId}/cancel")
    public ApiResponse<BillRequestOperationResponse> cancelRequest(@PathVariable Long bookId, @PathVariable Long requestId) {
        BillRequestOperationResult result = billRequestApplicationService.cancelRequest(CancelBillRequestCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .bookId(bookId)
            .requestId(requestId)
            .build());
        return ApiResponse.success(toOperationResponse(result));
    }

    private List<BillShareItemCommand> toShareItemCommands(List<CreateModifyBillRequestRequest.ShareItemRequest> requests) {
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

    private BillRequestDetailResponse toDetailResponse(BillRequestDetailResult result) {
        return BillRequestDetailResponse.builder()
            .requestId(result.getRequestId())
            .billId(result.getBillId())
            .predecessorRequestId(result.getPredecessorRequestId())
            .baseline(result.isBaseline())
            .requestType(result.getRequestType())
            .requesterMemberId(result.getRequesterMemberId())
            .requestReason(result.getRequestReason())
            .status(result.getStatus())
            .createdAt(result.getCreatedAt())
            .handledAt(result.getHandledAt())
            .currentUserCanApprove(result.isCurrentUserCanApprove())
            .currentUserCanCancel(result.isCurrentUserCanCancel())
            .snapshot(result.getSnapshot() == null ? null : BillRequestDetailResponse.SnapshotResponse.builder()
                .billType(result.getSnapshot().getBillType())
                .title(result.getSnapshot().getTitle())
                .billAmountCent(result.getSnapshot().getBillAmountCent())
                .categoryId(result.getSnapshot().getCategoryId())
                .categoryName(result.getSnapshot().getCategoryName())
                .categoryIcon(result.getSnapshot().getCategoryIcon())
                .payerMemberId(result.getSnapshot().getPayerMemberId())
                .payerMemberName(result.getSnapshot().getPayerMemberName())
                .recorderMemberId(result.getSnapshot().getRecorderMemberId())
                .recorderMemberName(result.getSnapshot().getRecorderMemberName())
                .tempParticipantId(result.getSnapshot().getTempParticipantId())
                .tempParticipantNickname(result.getSnapshot().getTempParticipantNickname())
                .billTime(result.getSnapshot().getBillTime())
                .remark(result.getSnapshot().getRemark())
                .attachmentUrls(result.getSnapshot().getAttachmentUrls() == null ? List.of() : result.getSnapshot().getAttachmentUrls())
                .shareItems(result.getSnapshot().getShareItems() == null ? List.of() : result.getSnapshot().getShareItems().stream()
                    .map(item -> BillRequestDetailResponse.SnapshotShareItemResponse.builder()
                        .participantType(item.getParticipantType())
                        .participantRefId(item.getParticipantRefId())
                        .participantName(item.getParticipantName())
                        .attachedMemberId(item.getAttachedMemberId())
                        .attachedMemberName(item.getAttachedMemberName())
                        .shareMethod(item.getShareMethod())
                        .shareRatio(item.getShareRatio())
                        .shareAmountCent(item.getShareAmountCent())
                        .build())
                    .toList())
                .build())
            .approvalRecords(result.getApprovalRecords() == null ? List.of() : result.getApprovalRecords().stream()
                .map(item -> BillRequestDetailResponse.ApprovalRecordResponse.builder()
                    .approvalId(item.getApprovalId())
                    .approverMemberId(item.getApproverMemberId())
                    .approvalAction(item.getApprovalAction())
                    .approvalComment(item.getApprovalComment())
                    .createdAt(item.getCreatedAt())
                    .build())
                .toList())
            .build();
    }

    private BillRequestOperationResponse toOperationResponse(BillRequestOperationResult result) {
        return BillRequestOperationResponse.builder()
            .requestId(result.getRequestId())
            .message(result.getMessage())
            .build();
    }
}
