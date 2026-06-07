package com.spvermicelli.tripledger.export.domain.model;

import com.spvermicelli.tripledger.shared.domain.enums.ExportType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 导出记录。
 * V1 先记录导出行为与导出文件地址，不把导出快照写入数据库。
 */
@Getter
@Builder
public class ExportRecord {

    private Long id;
    private Long bookId;
    private Long operatorMemberId;
    private ExportType exportType;
    private String fileUrl;
    private LocalDateTime createdAt;
}
