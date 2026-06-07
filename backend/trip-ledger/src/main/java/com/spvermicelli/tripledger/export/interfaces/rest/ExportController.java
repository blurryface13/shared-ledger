package com.spvermicelli.tripledger.export.interfaces.rest;

import com.spvermicelli.tripledger.export.application.ExportApplicationService;
import com.spvermicelli.tripledger.export.application.result.ExportRecordResult;
import com.spvermicelli.tripledger.export.interfaces.rest.response.ExportRecordResponse;
import com.spvermicelli.tripledger.shared.common.context.UserContextHolder;
import com.spvermicelli.tripledger.shared.common.response.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/books/{bookId}/exports")
public class ExportController {

    private final ExportApplicationService exportApplicationService;

    public ExportController(ExportApplicationService exportApplicationService) {
        this.exportApplicationService = exportApplicationService;
    }

    @PostMapping("/personal-detail")
    public ApiResponse<ExportRecordResponse> exportPersonalDetail(@PathVariable Long bookId) {
        return ApiResponse.success(toResponse(exportApplicationService.exportPersonalDetail(UserContextHolder.getUserId(), bookId)));
    }

    @PostMapping("/book-summary")
    public ApiResponse<ExportRecordResponse> exportBookSummary(@PathVariable Long bookId) {
        return ApiResponse.success(toResponse(exportApplicationService.exportBookSummary(UserContextHolder.getUserId(), bookId)));
    }

    @GetMapping
    public ApiResponse<List<ExportRecordResponse>> getExportRecords(@PathVariable Long bookId) {
        return ApiResponse.success(exportApplicationService.getExportRecords(UserContextHolder.getUserId(), bookId).stream()
            .map(this::toResponse)
            .toList());
    }

    private ExportRecordResponse toResponse(ExportRecordResult result) {
        return ExportRecordResponse.builder()
            .exportRecordId(result.getExportRecordId())
            .bookId(result.getBookId())
            .operatorMemberId(result.getOperatorMemberId())
            .exportType(result.getExportType())
            .fileUrl(result.getFileUrl())
            .exportContentJson(result.getExportContentJson())
            .createdAt(result.getCreatedAt())
            .build();
    }
}
