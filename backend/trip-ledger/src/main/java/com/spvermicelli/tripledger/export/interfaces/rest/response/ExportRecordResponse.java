package com.spvermicelli.tripledger.export.interfaces.rest.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExportRecordResponse {
    private Long exportRecordId;
    private Long bookId;
    private Long operatorMemberId;
    private String exportType;
    private String exportStatus;
    private String deliveryStatus;
    private Integer publishAttempts;
    private LocalDateTime nextPublishAttemptAt;
    private boolean retryable;
    private String fileUrl;
    private String exportContentJson;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
