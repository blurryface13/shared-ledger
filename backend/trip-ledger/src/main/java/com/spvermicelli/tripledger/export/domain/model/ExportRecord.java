package com.spvermicelli.tripledger.export.domain.model;

import com.spvermicelli.tripledger.shared.domain.enums.ExportType;
import com.spvermicelli.tripledger.shared.domain.enums.ExportStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 导出记录。
 * 异步任务保存不可变导出快照，后续预览不重新计算账目。
 */
@Getter
@Builder
public class ExportRecord {

    private Long id;
    private Long bookId;
    private Long operatorMemberId;
    private ExportType exportType;
    private ExportStatus exportStatus;
    private String fileUrl;
    private String exportContentJson;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
