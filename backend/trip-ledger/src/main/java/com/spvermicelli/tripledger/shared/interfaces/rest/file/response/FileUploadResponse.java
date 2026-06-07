package com.spvermicelli.tripledger.shared.interfaces.rest.file.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileUploadResponse {
    private String type;
    private String fileName;
    private String fileUrl;
    private String mimeType;
    private long fileSize;
}
