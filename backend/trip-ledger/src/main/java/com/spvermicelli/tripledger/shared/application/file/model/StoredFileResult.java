package com.spvermicelli.tripledger.shared.application.file.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoredFileResult {
    private String type;
    private String fileName;
    private String mimeType;
    private long fileSize;
}
