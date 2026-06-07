package com.spvermicelli.tripledger.settlement.interfaces.rest;

import com.spvermicelli.tripledger.settlement.application.SettlementApplicationService;
import com.spvermicelli.tripledger.settlement.application.command.CreateSettlementCommand;
import com.spvermicelli.tripledger.settlement.application.result.SettlementBatchResult;
import com.spvermicelli.tripledger.settlement.interfaces.rest.request.CreateSettlementRequest;
import com.spvermicelli.tripledger.settlement.interfaces.rest.response.SettlementBatchResponse;
import com.spvermicelli.tripledger.settlement.interfaces.rest.response.SettlementTransferResponse;
import com.spvermicelli.tripledger.shared.common.context.UserContextHolder;
import com.spvermicelli.tripledger.shared.common.response.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/books/{bookId}/settlements")
public class SettlementController {

    private final SettlementApplicationService settlementApplicationService;

    public SettlementController(SettlementApplicationService settlementApplicationService) {
        this.settlementApplicationService = settlementApplicationService;
    }

    @PostMapping
    public ApiResponse<SettlementBatchResponse> createSettlement(
        @PathVariable Long bookId,
        @RequestBody CreateSettlementRequest request
    ) {
        return ApiResponse.success(toResponse(settlementApplicationService.createSettlement(CreateSettlementCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .bookId(bookId)
            .strategyType(request.getStrategyType())
            .build())));
    }

    @GetMapping("/{settlementBatchId}")
    public ApiResponse<SettlementBatchResponse> getSettlementDetail(@PathVariable Long bookId, @PathVariable Long settlementBatchId) {
        return ApiResponse.success(toResponse(
            settlementApplicationService.getSettlementDetail(UserContextHolder.getUserId(), bookId, settlementBatchId)
        ));
    }

    @GetMapping
    public ApiResponse<List<SettlementBatchResponse>> getSettlementHistory(@PathVariable Long bookId) {
        return ApiResponse.success(settlementApplicationService.getSettlementHistory(UserContextHolder.getUserId(), bookId).stream()
            .map(this::toResponse)
            .toList());
    }

    private SettlementBatchResponse toResponse(SettlementBatchResult result) {
        return SettlementBatchResponse.builder()
            .settlementBatchId(result.getSettlementBatchId())
            .bookId(result.getBookId())
            .initiatorMemberId(result.getInitiatorMemberId())
            .strategyType(result.getStrategyType())
            .scopeType(result.getScopeType())
            .status(result.getStatus())
            .snapshotTime(result.getSnapshotTime())
            .createdAt(result.getCreatedAt())
            .transferList(result.getTransferList() == null ? List.of() : result.getTransferList().stream()
                .map(item -> SettlementTransferResponse.builder()
                    .transferId(item.getTransferId())
                    .fromMemberId(item.getFromMemberId())
                    .fromMemberName(item.getFromMemberName())
                    .toMemberId(item.getToMemberId())
                    .toMemberName(item.getToMemberName())
                    .transferAmountCent(item.getTransferAmountCent())
                    .relatedSummaryJson(item.getRelatedSummaryJson())
                    .build())
                .toList())
            .build();
    }
}
