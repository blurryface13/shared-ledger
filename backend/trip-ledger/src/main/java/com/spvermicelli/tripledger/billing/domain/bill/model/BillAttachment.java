package com.spvermicelli.tripledger.billing.domain.bill.model;

import com.spvermicelli.tripledger.shared.domain.enums.AttachmentFileType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BillAttachment {

    private Long id;
    private Long billId;
    private String fileUrl;
    private AttachmentFileType fileType;
    private Long uploadedByMemberId;
    private LocalDateTime createdAt;
}
