package com.spvermicelli.tripledger.ledger.interfaces.rest.invitation;

import com.spvermicelli.tripledger.ledger.application.invitation.BookInvitationApplicationService;
import com.spvermicelli.tripledger.ledger.application.invitation.command.AcceptInvitationCommand;
import com.spvermicelli.tripledger.ledger.application.invitation.command.CreateInvitationCommand;
import com.spvermicelli.tripledger.ledger.application.invitation.command.RejectInvitationCommand;
import com.spvermicelli.tripledger.ledger.application.invitation.command.RevokeInvitationCommand;
import com.spvermicelli.tripledger.ledger.application.invitation.result.CreateInvitationResult;
import com.spvermicelli.tripledger.ledger.application.invitation.result.InvitationCandidateResult;
import com.spvermicelli.tripledger.ledger.application.invitation.result.InvitationMessageItemResult;
import com.spvermicelli.tripledger.ledger.application.invitation.result.InvitationOperationResult;
import com.spvermicelli.tripledger.ledger.application.invitation.result.PendingInvitationResult;
import com.spvermicelli.tripledger.ledger.interfaces.rest.invitation.request.CreateInvitationRequest;
import com.spvermicelli.tripledger.ledger.interfaces.rest.invitation.response.CreateInvitationResponse;
import com.spvermicelli.tripledger.ledger.interfaces.rest.invitation.response.InvitationCandidateResponse;
import com.spvermicelli.tripledger.ledger.interfaces.rest.invitation.response.InvitationMessageItemResponse;
import com.spvermicelli.tripledger.ledger.interfaces.rest.invitation.response.InvitationOperationResponse;
import com.spvermicelli.tripledger.ledger.interfaces.rest.invitation.response.PendingInvitationResponse;
import com.spvermicelli.tripledger.shared.common.context.UserContextHolder;
import com.spvermicelli.tripledger.shared.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账本邀请接口。
 * 该控制器只暴露 HTTP 契约，真正的鉴权、邀请状态流转和成员恢复逻辑全部放在应用服务中执行。
 */
@RestController
@RequestMapping("/api/v1")
public class BookInvitationController {

    private final BookInvitationApplicationService bookInvitationApplicationService;

    public BookInvitationController(BookInvitationApplicationService bookInvitationApplicationService) {
        this.bookInvitationApplicationService = bookInvitationApplicationService;
    }

    @PostMapping("/books/{bookId}/invitations")
    public ApiResponse<CreateInvitationResponse> createInvitation(
        @PathVariable Long bookId,
        @Valid @RequestBody CreateInvitationRequest request
    ) {
        CreateInvitationResult result = bookInvitationApplicationService.createInvitation(CreateInvitationCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .bookId(bookId)
            .inviteeUserId(request.getInviteeUserId())
            .remark(request.getRemark())
            .build());

        return ApiResponse.success(CreateInvitationResponse.builder()
            .invitationId(result.getInvitationId())
            .status(result.getStatus())
            .expireAt(result.getExpireAt())
            .message(result.getMessage())
            .build());
    }

    @GetMapping("/books/{bookId}/invitation-candidates")
    public ApiResponse<List<InvitationCandidateResponse>> searchInvitationCandidates(
        @PathVariable Long bookId,
        @RequestParam String searchType,
        @RequestParam String keyword
    ) {
        List<InvitationCandidateResponse> response = bookInvitationApplicationService.searchInvitationCandidates(
                UserContextHolder.getUserId(),
                bookId,
                searchType,
                keyword
            ).stream()
            .map(this::toCandidateResponse)
            .toList();
        return ApiResponse.success(response);
    }

    @GetMapping("/invitations/pending")
    public ApiResponse<List<PendingInvitationResponse>> getPendingInvitations() {
        List<PendingInvitationResponse> response = bookInvitationApplicationService.getPendingInvitations(
                UserContextHolder.getUserId()
            ).stream()
            .map(this::toPendingResponse)
            .toList();
        return ApiResponse.success(response);
    }

    @GetMapping("/invitations/received")
    public ApiResponse<List<InvitationMessageItemResponse>> getReceivedInvitations() {
        List<InvitationMessageItemResponse> response = bookInvitationApplicationService.getReceivedInvitations(
                UserContextHolder.getUserId()
            ).stream()
            .map(this::toMessageResponse)
            .toList();
        return ApiResponse.success(response);
    }

    @GetMapping("/invitations/sent")
    public ApiResponse<List<InvitationMessageItemResponse>> getSentInvitations() {
        List<InvitationMessageItemResponse> response = bookInvitationApplicationService.getSentInvitations(
                UserContextHolder.getUserId()
            ).stream()
            .map(this::toMessageResponse)
            .toList();
        return ApiResponse.success(response);
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public ApiResponse<InvitationOperationResponse> acceptInvitation(@PathVariable Long invitationId) {
        InvitationOperationResult result = bookInvitationApplicationService.acceptInvitation(AcceptInvitationCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .invitationId(invitationId)
            .build());
        return ApiResponse.success(toOperationResponse(result));
    }

    @PostMapping("/invitations/{invitationId}/reject")
    public ApiResponse<InvitationOperationResponse> rejectInvitation(@PathVariable Long invitationId) {
        InvitationOperationResult result = bookInvitationApplicationService.rejectInvitation(RejectInvitationCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .invitationId(invitationId)
            .build());
        return ApiResponse.success(toOperationResponse(result));
    }

    @PostMapping("/invitations/{invitationId}/revoke")
    public ApiResponse<InvitationOperationResponse> revokeInvitation(@PathVariable Long invitationId) {
        InvitationOperationResult result = bookInvitationApplicationService.revokeInvitation(RevokeInvitationCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .invitationId(invitationId)
            .build());
        return ApiResponse.success(toOperationResponse(result));
    }

    private PendingInvitationResponse toPendingResponse(PendingInvitationResult result) {
        return PendingInvitationResponse.builder()
            .invitationId(result.getInvitationId())
            .bookId(result.getBookId())
            .bookName(result.getBookName())
            .inviterUserId(result.getInviterUserId())
            .inviterNickname(result.getInviterNickname())
            .inviterAvatarUrl(result.getInviterAvatarUrl())
            .status(result.getStatus())
            .invitedAt(result.getInvitedAt())
            .expireAt(result.getExpireAt())
            .remark(result.getRemark())
            .build();
    }

    private InvitationOperationResponse toOperationResponse(InvitationOperationResult result) {
        return InvitationOperationResponse.builder()
            .invitationId(result.getInvitationId())
            .status(result.getStatus())
            .message(result.getMessage())
            .build();
    }

    private InvitationCandidateResponse toCandidateResponse(InvitationCandidateResult result) {
        return InvitationCandidateResponse.builder()
            .userId(result.getUserId())
            .nickname(result.getNickname())
            .avatarUrl(result.getAvatarUrl())
            .phoneNumber(result.getPhoneNumber())
            .pendingInvitation(result.isPendingInvitation())
            .build();
    }

    private InvitationMessageItemResponse toMessageResponse(InvitationMessageItemResult result) {
        return InvitationMessageItemResponse.builder()
            .invitationId(result.getInvitationId())
            .bookId(result.getBookId())
            .bookName(result.getBookName())
            .bookCoverUrl(result.getBookCoverUrl())
            .status(result.getStatus())
            .invitedAt(result.getInvitedAt())
            .handledAt(result.getHandledAt())
            .expireAt(result.getExpireAt())
            .remark(result.getRemark())
            .inviterUserId(result.getInviterUserId())
            .inviterNickname(result.getInviterNickname())
            .inviterAvatarUrl(result.getInviterAvatarUrl())
            .inviterPhoneNumber(result.getInviterPhoneNumber())
            .inviteeUserId(result.getInviteeUserId())
            .inviteeNickname(result.getInviteeNickname())
            .inviteeAvatarUrl(result.getInviteeAvatarUrl())
            .inviteePhoneNumber(result.getInviteePhoneNumber())
            .canRevoke(result.isCanRevoke())
            .build();
    }
}
