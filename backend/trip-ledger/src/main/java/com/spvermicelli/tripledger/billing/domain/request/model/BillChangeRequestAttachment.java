package com.spvermicelli.tripledger.billing.domain.request.model;

import com.spvermicelli.tripledger.shared.domain.enums.AttachmentFileType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BillChangeRequestAttachment {

    private Long id;
    private Long requestId;
    private String fileUrl;
    private AttachmentFileType fileType;
    private Long uploadedByMemberId;
    private LocalDateTime createdAt;
}
