package com.spvermicelli.tripledger.ledger.interfaces.rest.member;

import com.spvermicelli.tripledger.ledger.application.member.BookMemberApplicationService;
import com.spvermicelli.tripledger.ledger.application.member.command.CancelBookAdminCommand;
import com.spvermicelli.tripledger.ledger.application.member.command.QuitBookCommand;
import com.spvermicelli.tripledger.ledger.application.member.command.RemoveBookMemberCommand;
import com.spvermicelli.tripledger.ledger.application.member.command.SetBookAdminCommand;
import com.spvermicelli.tripledger.ledger.application.member.command.TransferBookOwnerCommand;
import com.spvermicelli.tripledger.ledger.application.member.result.BookMemberListItemResult;
import com.spvermicelli.tripledger.ledger.application.member.result.MemberOperationResult;
import com.spvermicelli.tripledger.ledger.interfaces.rest.member.request.TransferOwnerRequest;
import com.spvermicelli.tripledger.ledger.interfaces.rest.member.response.BookMemberListItemResponse;
import com.spvermicelli.tripledger.ledger.interfaces.rest.member.response.MemberOperationResponse;
import com.spvermicelli.tripledger.shared.common.context.UserContextHolder;
import com.spvermicelli.tripledger.shared.common.response.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账本成员接口。
 * 本控制器中的所有身份判断都只信任后端解析出的 token 用户，不接受前端越权指定操作者。
 */
@RestController
@RequestMapping("/api/v1/books")
public class BookMemberController {

    private final BookMemberApplicationService bookMemberApplicationService;

    public BookMemberController(BookMemberApplicationService bookMemberApplicationService) {
        this.bookMemberApplicationService = bookMemberApplicationService;
    }

    @GetMapping("/{bookId}/members")
    public ApiResponse<List<BookMemberListItemResponse>> getBookMembers(@PathVariable Long bookId) {
        List<BookMemberListItemResponse> response = bookMemberApplicationService.getBookMembers(
                UserContextHolder.getUserId(),
                bookId
            ).stream()
            .map(this::toResponse)
            .toList();
        return ApiResponse.success(response);
    }

    @PostMapping("/{bookId}/quit")
    public ApiResponse<MemberOperationResponse> quitBook(@PathVariable Long bookId) {
        MemberOperationResult result = bookMemberApplicationService.quitBook(QuitBookCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .bookId(bookId)
            .build());
        return ApiResponse.success(MemberOperationResponse.builder().message(result.getMessage()).build());
    }

    @PostMapping("/{bookId}/members/{memberId}/remove")
    public ApiResponse<MemberOperationResponse> removeMember(@PathVariable Long bookId, @PathVariable Long memberId) {
        MemberOperationResult result = bookMemberApplicationService.removeMember(RemoveBookMemberCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .bookId(bookId)
            .memberId(memberId)
            .build());
        return ApiResponse.success(MemberOperationResponse.builder().message(result.getMessage()).build());
    }

    @PostMapping("/{bookId}/members/{memberId}/set-admin")
    public ApiResponse<MemberOperationResponse> setAdmin(@PathVariable Long bookId, @PathVariable Long memberId) {
        MemberOperationResult result = bookMemberApplicationService.setAdmin(SetBookAdminCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .bookId(bookId)
            .memberId(memberId)
            .build());
        return ApiResponse.success(MemberOperationResponse.builder().message(result.getMessage()).build());
    }

    @PostMapping("/{bookId}/members/{memberId}/cancel-admin")
    public ApiResponse<MemberOperationResponse> cancelAdmin(@PathVariable Long bookId, @PathVariable Long memberId) {
        MemberOperationResult result = bookMemberApplicationService.cancelAdmin(CancelBookAdminCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .bookId(bookId)
            .memberId(memberId)
            .build());
        return ApiResponse.success(MemberOperationResponse.builder().message(result.getMessage()).build());
    }

    @PostMapping("/{bookId}/transfer-owner")
    public ApiResponse<MemberOperationResponse> transferOwner(
        @PathVariable Long bookId,
        @RequestBody TransferOwnerRequest request
    ) {
        MemberOperationResult result = bookMemberApplicationService.transferOwner(TransferBookOwnerCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .bookId(bookId)
            .targetMemberId(request.getTargetMemberId())
            .build());
        return ApiResponse.success(MemberOperationResponse.builder().message(result.getMessage()).build());
    }

    private BookMemberListItemResponse toResponse(BookMemberListItemResult result) {
        return BookMemberListItemResponse.builder()
            .memberId(result.getMemberId())
            .userId(result.getUserId())
            .nickname(result.getNickname())
            .avatarUrl(result.getAvatarUrl())
            .phoneNumber(result.getPhoneNumber())
            .memberRole(result.getMemberRole())
            .memberStatus(result.getMemberStatus())
            .joinedAt(result.getJoinedAt())
            .leftAt(result.getLeftAt())
            .build();
    }
}
