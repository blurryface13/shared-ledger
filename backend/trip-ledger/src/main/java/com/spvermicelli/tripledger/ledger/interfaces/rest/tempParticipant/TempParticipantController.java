package com.spvermicelli.tripledger.ledger.interfaces.rest.tempParticipant;

import com.spvermicelli.tripledger.ledger.application.tempParticipant.TempParticipantApplicationService;
import com.spvermicelli.tripledger.ledger.application.tempParticipant.command.ChangeTempParticipantAttachedMemberCommand;
import com.spvermicelli.tripledger.ledger.application.tempParticipant.command.CreateTempParticipantCommand;
import com.spvermicelli.tripledger.ledger.application.tempParticipant.command.DeleteTempParticipantCommand;
import com.spvermicelli.tripledger.ledger.application.tempParticipant.command.EnableTempParticipantCommand;
import com.spvermicelli.tripledger.ledger.application.tempParticipant.command.UpdateTempParticipantNicknameCommand;
import com.spvermicelli.tripledger.ledger.application.tempParticipant.result.TempParticipantDetailResult;
import com.spvermicelli.tripledger.ledger.application.tempParticipant.result.TempParticipantMemberResult;
import com.spvermicelli.tripledger.ledger.application.tempParticipant.result.TempParticipantOperationResult;
import com.spvermicelli.tripledger.ledger.interfaces.rest.tempParticipant.request.ChangeTempParticipantAttachedMemberRequest;
import com.spvermicelli.tripledger.ledger.interfaces.rest.tempParticipant.request.CreateTempParticipantRequest;
import com.spvermicelli.tripledger.ledger.interfaces.rest.tempParticipant.request.UpdateTempParticipantNicknameRequest;
import com.spvermicelli.tripledger.ledger.interfaces.rest.tempParticipant.response.TempParticipantDetailResponse;
import com.spvermicelli.tripledger.ledger.interfaces.rest.tempParticipant.response.TempParticipantMemberResponse;
import com.spvermicelli.tripledger.ledger.interfaces.rest.tempParticipant.response.TempParticipantOperationResponse;
import com.spvermicelli.tripledger.shared.common.context.UserContextHolder;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.spvermicelli.tripledger.shared.common.response.ApiResponse;
import com.spvermicelli.tripledger.shared.domain.enums.TempParticipantType;
import jakarta.validation.Valid;
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
 * 临时成员接口。
 * 控制器只负责协议解析与枚举转换，具体的权限判断、可见性过滤和业务规则均由应用服务负责。
 */
@RestController
@RequestMapping("/api/v1/books/{bookId}/temp-participants")
public class TempParticipantController {

    private final TempParticipantApplicationService tempParticipantApplicationService;

    public TempParticipantController(TempParticipantApplicationService tempParticipantApplicationService) {
        this.tempParticipantApplicationService = tempParticipantApplicationService;
    }

    @GetMapping
    public ApiResponse<List<TempParticipantDetailResponse>> getTempParticipants(
        @PathVariable Long bookId,
        @RequestParam(required = false) String tempType
    ) {
        List<TempParticipantDetailResponse> response = tempParticipantApplicationService.getTempParticipants(
                UserContextHolder.getUserId(),
                bookId,
                parseTempType(tempType)
            ).stream()
            .map(this::toDetailResponse)
            .toList();
        return ApiResponse.success(response);
    }

    @PostMapping
    public ApiResponse<TempParticipantOperationResponse> createTempParticipant(
        @PathVariable Long bookId,
        @Valid @RequestBody CreateTempParticipantRequest request
    ) {
        TempParticipantOperationResult result = tempParticipantApplicationService.createTempParticipant(
            CreateTempParticipantCommand.builder()
                .currentUserId(UserContextHolder.getUserId())
                .bookId(bookId)
                .nickname(request.getNickname())
                .tempType(parseTempType(request.getTempType()))
                .attachedMemberId(request.getAttachedMemberId())
                .build()
        );
        return ApiResponse.success(toOperationResponse(result));
    }

    @PutMapping("/{tempParticipantId}/nickname")
    public ApiResponse<TempParticipantOperationResponse> updateTempParticipantNickname(
        @PathVariable Long bookId,
        @PathVariable Long tempParticipantId,
        @Valid @RequestBody UpdateTempParticipantNicknameRequest request
    ) {
        TempParticipantOperationResult result = tempParticipantApplicationService.updateNickname(
            UpdateTempParticipantNicknameCommand.builder()
                .currentUserId(UserContextHolder.getUserId())
                .bookId(bookId)
                .tempParticipantId(tempParticipantId)
                .nickname(request.getNickname())
                .build()
        );
        return ApiResponse.success(toOperationResponse(result));
    }

    @PutMapping("/{tempParticipantId}/attached-member")
    public ApiResponse<TempParticipantOperationResponse> changeAttachedMember(
        @PathVariable Long bookId,
        @PathVariable Long tempParticipantId,
        @Valid @RequestBody ChangeTempParticipantAttachedMemberRequest request
    ) {
        TempParticipantOperationResult result = tempParticipantApplicationService.changeAttachedMember(
            ChangeTempParticipantAttachedMemberCommand.builder()
                .currentUserId(UserContextHolder.getUserId())
                .bookId(bookId)
                .tempParticipantId(tempParticipantId)
                .attachedMemberId(request.getAttachedMemberId())
                .build()
        );
        return ApiResponse.success(toOperationResponse(result));
    }

    @DeleteMapping("/{tempParticipantId}")
    public ApiResponse<TempParticipantOperationResponse> deleteTempParticipant(
        @PathVariable Long bookId,
        @PathVariable Long tempParticipantId
    ) {
        TempParticipantOperationResult result = tempParticipantApplicationService.deleteTempParticipant(
            DeleteTempParticipantCommand.builder()
                .currentUserId(UserContextHolder.getUserId())
                .bookId(bookId)
                .tempParticipantId(tempParticipantId)
                .build()
        );
        return ApiResponse.success(toOperationResponse(result));
    }

    @PostMapping("/{tempParticipantId}/enable")
    public ApiResponse<TempParticipantOperationResponse> enableTempParticipant(
        @PathVariable Long bookId,
        @PathVariable Long tempParticipantId
    ) {
        TempParticipantOperationResult result = tempParticipantApplicationService.enableTempParticipant(
            EnableTempParticipantCommand.builder()
                .currentUserId(UserContextHolder.getUserId())
                .bookId(bookId)
                .tempParticipantId(tempParticipantId)
                .build()
        );
        return ApiResponse.success(toOperationResponse(result));
    }

    private TempParticipantDetailResponse toDetailResponse(TempParticipantDetailResult result) {
        return TempParticipantDetailResponse.builder()
            .tempParticipantId(result.getTempParticipantId())
            .nickname(result.getNickname())
            .tempType(result.getTempType())
            .status(result.getStatus())
            .createdByMember(toMemberResponse(result.getCreatedByMember()))
            .attachedMember(toMemberResponse(result.getAttachedMember()))
            .createdAt(result.getCreatedAt())
            .updatedAt(result.getUpdatedAt())
            .build();
    }

    private TempParticipantOperationResponse toOperationResponse(TempParticipantOperationResult result) {
        return TempParticipantOperationResponse.builder()
            .tempParticipantId(result.getTempParticipantId())
            .message(result.getMessage())
            .build();
    }

    private TempParticipantMemberResponse toMemberResponse(TempParticipantMemberResult result) {
        if (result == null) {
            return null;
        }
        return TempParticipantMemberResponse.builder()
            .memberId(result.getMemberId())
            .userId(result.getUserId())
            .nickname(result.getNickname())
            .avatarUrl(result.getAvatarUrl())
            .phoneNumber(result.getPhoneNumber())
            .build();
    }

    private TempParticipantType parseTempType(String rawType) {
        if (rawType == null) {
            return null;
        }
        TempParticipantType parsedType = TempParticipantType.parseOrNull(rawType);
        if (parsedType == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "tempType 仅支持 PRIVATE 或 GLOBAL");
        }
        return parsedType;
    }
}
