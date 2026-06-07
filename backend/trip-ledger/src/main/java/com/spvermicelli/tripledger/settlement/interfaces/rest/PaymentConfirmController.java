package com.spvermicelli.tripledger.settlement.interfaces.rest;

import com.spvermicelli.tripledger.settlement.application.PaymentConfirmApplicationService;
import com.spvermicelli.tripledger.settlement.application.command.ConfirmPaymentCommand;
import com.spvermicelli.tripledger.settlement.application.command.CreateDirectReceiveCommand;
import com.spvermicelli.tripledger.settlement.application.command.CreatePaymentConfirmCommand;
import com.spvermicelli.tripledger.settlement.application.command.RejectPaymentCommand;
import com.spvermicelli.tripledger.settlement.application.result.PaymentConfirmResult;
import com.spvermicelli.tripledger.settlement.interfaces.rest.request.CreateDirectReceiveRequest;
import com.spvermicelli.tripledger.settlement.interfaces.rest.request.CreatePaymentConfirmRequest;
import com.spvermicelli.tripledger.settlement.interfaces.rest.request.RejectPaymentRequest;
import com.spvermicelli.tripledger.settlement.interfaces.rest.response.PaymentConfirmAllocationResponse;
import com.spvermicelli.tripledger.settlement.interfaces.rest.response.PaymentConfirmResponse;
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

@RestController
@RequestMapping("/api/v1/books/{bookId}/payment-confirms")
public class PaymentConfirmController {

    private final PaymentConfirmApplicationService paymentConfirmApplicationService;

    public PaymentConfirmController(PaymentConfirmApplicationService paymentConfirmApplicationService) {
        this.paymentConfirmApplicationService = paymentConfirmApplicationService;
    }

    @PostMapping
    public ApiResponse<PaymentConfirmResponse> createPaymentConfirm(
        @PathVariable Long bookId,
        @Valid @RequestBody CreatePaymentConfirmRequest request
    ) {
        return ApiResponse.success(toResponse(paymentConfirmApplicationService.createPaymentConfirm(CreatePaymentConfirmCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .bookId(bookId)
            .toMemberId(request.getToMemberId())
            .paymentAmountCent(request.getPaymentAmountCent())
            .sourceType(request.getSourceType())
            .sourceRefId(request.getSourceRefId())
            .remark(request.getRemark())
            .build())));
    }

    @GetMapping("/pending-receive")
    public ApiResponse<List<PaymentConfirmResponse>> getPendingReceive(@PathVariable Long bookId) {
        return ApiResponse.success(paymentConfirmApplicationService.getPendingReceiveList(UserContextHolder.getUserId(), bookId).stream()
            .map(this::toResponse)
            .toList());
    }

    @PostMapping("/{paymentConfirmId}/confirm")
    public ApiResponse<PaymentConfirmResponse> confirmPayment(@PathVariable Long bookId, @PathVariable Long paymentConfirmId) {
        return ApiResponse.success(toResponse(paymentConfirmApplicationService.confirmPayment(ConfirmPaymentCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .bookId(bookId)
            .paymentConfirmId(paymentConfirmId)
            .build())));
    }

    @PostMapping("/direct-receive")
    public ApiResponse<PaymentConfirmResponse> createDirectReceive(
        @PathVariable Long bookId,
        @Valid @RequestBody CreateDirectReceiveRequest request
    ) {
        return ApiResponse.success(toResponse(paymentConfirmApplicationService.createDirectReceive(CreateDirectReceiveCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .bookId(bookId)
            .fromMemberId(request.getFromMemberId())
            .paymentAmountCent(request.getPaymentAmountCent())
            .sourceType(request.getSourceType())
            .sourceRefId(request.getSourceRefId())
            .remark(request.getRemark())
            .build())));
    }

    @GetMapping("/pending-pay")
    public ApiResponse<List<PaymentConfirmResponse>> getPendingPay(@PathVariable Long bookId) {
        return ApiResponse.success(paymentConfirmApplicationService.getPendingPayList(UserContextHolder.getUserId(), bookId).stream()
            .map(this::toResponse)
            .toList());
    }

    @PostMapping("/{paymentConfirmId}/reject")
    public ApiResponse<PaymentConfirmResponse> rejectPayment(
        @PathVariable Long bookId,
        @PathVariable Long paymentConfirmId,
        @Valid @RequestBody(required = false) RejectPaymentRequest request
    ) {
        return ApiResponse.success(toResponse(paymentConfirmApplicationService.rejectPayment(RejectPaymentCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .bookId(bookId)
            .paymentConfirmId(paymentConfirmId)
            .rejectRemark(request == null ? null : request.getRejectRemark())
            .build())));
    }

    @GetMapping("/history")
    public ApiResponse<List<PaymentConfirmResponse>> getRelationHistory(
        @PathVariable Long bookId,
        @RequestParam(required = false) Long targetMemberId,
        @RequestParam(required = false) String direction
    ) {
        return ApiResponse.success(paymentConfirmApplicationService.getRelationHistory(
                UserContextHolder.getUserId(),
                bookId,
                targetMemberId,
                direction
            ).stream()
            .map(this::toResponse)
            .toList());
    }

    private PaymentConfirmResponse toResponse(PaymentConfirmResult result) {
        return PaymentConfirmResponse.builder()
            .paymentConfirmId(result.getPaymentConfirmId())
            .bookId(result.getBookId())
            .fromMemberId(result.getFromMemberId())
            .fromMemberName(result.getFromMemberName())
            .toMemberId(result.getToMemberId())
            .toMemberName(result.getToMemberName())
            .paymentAmountCent(result.getPaymentAmountCent())
            .sourceType(result.getSourceType())
            .sourceRefId(result.getSourceRefId())
            .confirmStatus(result.getConfirmStatus())
            .initiatedByMemberId(result.getInitiatedByMemberId())
            .confirmedByMemberId(result.getConfirmedByMemberId())
            .initiatedAt(result.getInitiatedAt())
            .confirmedAt(result.getConfirmedAt())
            .autoConfirmAt(result.getAutoConfirmAt())
            .remark(result.getRemark())
            .allocationList(result.getAllocationList() == null ? List.of() : result.getAllocationList().stream()
                .map(item -> PaymentConfirmAllocationResponse.builder()
                    .billId(item.getBillId())
                    .allocatedAmountCent(item.getAllocatedAmountCent())
                    .build())
                .toList())
            .build();
    }
}
