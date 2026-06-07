package com.spvermicelli.tripledger.settlement.interfaces.rest;

import com.spvermicelli.tripledger.settlement.application.TempRecoveryApplicationService;
import com.spvermicelli.tripledger.settlement.application.command.CreateTempRecoveryCommand;
import com.spvermicelli.tripledger.settlement.application.result.TempRecoveryResult;
import com.spvermicelli.tripledger.settlement.interfaces.rest.request.CreateTempRecoveryRequest;
import com.spvermicelli.tripledger.settlement.interfaces.rest.response.TempRecoveryAllocationResponse;
import com.spvermicelli.tripledger.settlement.interfaces.rest.response.TempRecoveryResponse;
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
@RequestMapping("/api/v1/books/{bookId}/temp-recoveries")
public class TempRecoveryController {

    private final TempRecoveryApplicationService tempRecoveryApplicationService;

    public TempRecoveryController(TempRecoveryApplicationService tempRecoveryApplicationService) {
        this.tempRecoveryApplicationService = tempRecoveryApplicationService;
    }

    @PostMapping
    public ApiResponse<TempRecoveryResponse> createTempRecovery(
        @PathVariable Long bookId,
        @Valid @RequestBody CreateTempRecoveryRequest request
    ) {
        return ApiResponse.success(toResponse(tempRecoveryApplicationService.createTempRecovery(CreateTempRecoveryCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .bookId(bookId)
            .tempParticipantId(request.getTempParticipantId())
            .recoveryAmountCent(request.getRecoveryAmountCent())
            .remark(request.getRemark())
            .build())));
    }

    @GetMapping
    public ApiResponse<List<TempRecoveryResponse>> getTempRecoveries(@PathVariable Long bookId) {
        return ApiResponse.success(tempRecoveryApplicationService.getTempRecoveryList(UserContextHolder.getUserId(), bookId).stream()
            .map(this::toResponse)
            .toList());
    }

    private TempRecoveryResponse toResponse(TempRecoveryResult result) {
        return TempRecoveryResponse.builder()
            .recoveryRecordId(result.getRecoveryRecordId())
            .bookId(result.getBookId())
            .tempParticipantId(result.getTempParticipantId())
            .tempParticipantNickname(result.getTempParticipantNickname())
            .attachedMemberId(result.getAttachedMemberId())
            .recoveryAmountCent(result.getRecoveryAmountCent())
            .confirmStatus(result.getConfirmStatus())
            .confirmedByMemberId(result.getConfirmedByMemberId())
            .createdAt(result.getCreatedAt())
            .confirmedAt(result.getConfirmedAt())
            .remark(result.getRemark())
            .allocationList(result.getAllocationList() == null ? List.of() : result.getAllocationList().stream()
                .map(item -> TempRecoveryAllocationResponse.builder()
                    .billId(item.getBillId())
                    .allocatedAmountCent(item.getAllocatedAmountCent())
                    .build())
                .toList())
            .build();
    }
}
