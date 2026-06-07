package com.spvermicelli.tripledger.billing.domain.bill.repository;

import com.spvermicelli.tripledger.billing.domain.bill.model.BillAttachment;
import java.util.List;

public interface BillAttachmentRepository {

    List<BillAttachment> findByBillId(Long billId);

    void replaceAll(Long billId, Long uploadedByMemberId, List<String> attachmentUrls);
}
