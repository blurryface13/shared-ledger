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
    private String fileUrl;
    private String exportContentJson;
    private LocalDateTime createdAt;
}
