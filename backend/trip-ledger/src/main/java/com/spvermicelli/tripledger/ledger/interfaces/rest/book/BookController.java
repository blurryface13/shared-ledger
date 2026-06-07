package com.spvermicelli.tripledger.ledger.interfaces.rest.book;

import com.spvermicelli.tripledger.ledger.application.book.BookApplicationService;
import com.spvermicelli.tripledger.ledger.application.book.command.CreateBookCommand;
import com.spvermicelli.tripledger.ledger.application.book.command.DeleteBookCommand;
import com.spvermicelli.tripledger.ledger.application.book.command.UpdateBookCommand;
import com.spvermicelli.tripledger.ledger.application.book.result.BookDetailResult;
import com.spvermicelli.tripledger.ledger.application.book.result.BookListItemResult;
import com.spvermicelli.tripledger.ledger.application.book.result.CreateBookResult;
import com.spvermicelli.tripledger.ledger.interfaces.rest.book.request.CreateBookRequest;
import com.spvermicelli.tripledger.ledger.interfaces.rest.book.request.UpdateBookRequest;
import com.spvermicelli.tripledger.ledger.interfaces.rest.book.response.BookDetailResponse;
import com.spvermicelli.tripledger.ledger.interfaces.rest.book.response.BookListItemResponse;
import com.spvermicelli.tripledger.ledger.interfaces.rest.book.response.BookMemberResponse;
import com.spvermicelli.tripledger.ledger.interfaces.rest.book.response.CreateBookResponse;
import com.spvermicelli.tripledger.ledger.interfaces.rest.book.response.TempParticipantResponse;
import com.spvermicelli.tripledger.shared.common.context.UserContextHolder;
import com.spvermicelli.tripledger.shared.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账本接口。
 * 当前所有账本接口都基于 access token 中的当前用户身份执行权限判断。
 */
@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    private final BookApplicationService bookApplicationService;

    public BookController(BookApplicationService bookApplicationService) {
        this.bookApplicationService = bookApplicationService;
    }

    /**
     * 创建新账本
     * @param request
     * @return
     */
    @PostMapping
    public ApiResponse<CreateBookResponse> createBook(@Valid @RequestBody CreateBookRequest request) {
        CreateBookResult result = bookApplicationService.createBook(CreateBookCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .name(request.getName())
            .bookType(request.getBookType())
            .description(request.getDescription())
            .coverUrl(request.getCoverUrl())
            .build());
        return ApiResponse.success(CreateBookResponse.builder().bookId(result.getBookId()).build());
    }

    /**
     * 查询我的账本列表
     * @return
     */
    @GetMapping("/my")
    public ApiResponse<List<BookListItemResponse>> getMyBooks() {
        List<BookListItemResponse> response = bookApplicationService.getMyBooks(UserContextHolder.getUserId())
            .stream()
            .map(this::toListItemResponse)
            .toList();
        return ApiResponse.success(response);
    }

    /**
     * 获取账本详情
     * @param bookId
     * @return
     */
    @GetMapping("/{bookId}")
    public ApiResponse<BookDetailResponse> getBookDetail(@PathVariable Long bookId) {
        return ApiResponse.success(toDetailResponse(
            bookApplicationService.getBookDetail(UserContextHolder.getUserId(), bookId)
        ));
    }

    /**
     * 更新账本
     * @param bookId
     * @param request
     * @return
     */
    @PutMapping("/{bookId}")
    public ApiResponse<Void> updateBook(@PathVariable Long bookId, @Valid @RequestBody UpdateBookRequest request) {
        bookApplicationService.updateBook(UpdateBookCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .bookId(bookId)
            .name(request.getName())
            .description(request.getDescription())
            .coverUrl(request.getCoverUrl())
            .build());
        return ApiResponse.success();
    }

    @DeleteMapping("/{bookId}")
    public ApiResponse<Void> deleteBook(@PathVariable Long bookId) {
        bookApplicationService.deleteBook(DeleteBookCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .bookId(bookId)
            .build());
        return ApiResponse.success();
    }

    private BookListItemResponse toListItemResponse(BookListItemResult result) {
        return BookListItemResponse.builder()
            .bookId(result.getBookId())
            .name(result.getName())
            .bookType(result.getBookType())
            .description(result.getDescription())
            .coverUrl(result.getCoverUrl())
            .createdAt(result.getCreatedAt())
            .build();
    }

    private BookDetailResponse toDetailResponse(BookDetailResult result) {
        return BookDetailResponse.builder()
            .bookId(result.getBookId())
            .name(result.getName())
            .bookType(result.getBookType())
            .description(result.getDescription())
            .coverUrl(result.getCoverUrl())
            .ownerUserId(result.getOwnerUserId())
            .currentMemberId(result.getCurrentMemberId())
            .currentMemberRole(result.getCurrentMemberRole())
            .currentMemberStatus(result.getCurrentMemberStatus())
            .shared(result.isShared())
            .canEditBook(result.isCanEditBook())
            .canInviteMember(result.isCanInviteMember())
            .canRemoveMember(result.isCanRemoveMember())
            .canCancelAdmin(result.isCanCancelAdmin())
            .canTransferOwner(result.isCanTransferOwner())
            .canQuitBook(result.isCanQuitBook())
            .canDeleteBook(result.isCanDeleteBook())
            .members(result.getMembers().stream()
                .map(member -> BookMemberResponse.builder()
                    .memberId(member.getMemberId())
                    .userId(member.getUserId())
                    .nickname(member.getNickname())
                    .avatarUrl(member.getAvatarUrl())
                    .phoneNumber(member.getPhoneNumber())
                    .memberRole(member.getMemberRole())
                    .memberStatus(member.getMemberStatus())
                    .joinedAt(member.getJoinedAt())
                    .leftAt(member.getLeftAt())
                    .build())
                .toList())
            .tempParticipants(result.getTempParticipants().stream()
                .map(tempParticipant -> TempParticipantResponse.builder()
                    .tempParticipantId(tempParticipant.getTempParticipantId())
                    .nickname(tempParticipant.getNickname())
                    .tempType(tempParticipant.getTempType())
                    .status(tempParticipant.getStatus())
                    .attachedMemberId(tempParticipant.getAttachedMemberId())
                    .attachedUserId(tempParticipant.getAttachedUserId())
                    .attachedNickname(tempParticipant.getAttachedNickname())
                    .attachedAvatarUrl(tempParticipant.getAttachedAvatarUrl())
                    .attachedPhoneNumber(tempParticipant.getAttachedPhoneNumber())
                    .build())
                .toList())
            .build();
    }
}
