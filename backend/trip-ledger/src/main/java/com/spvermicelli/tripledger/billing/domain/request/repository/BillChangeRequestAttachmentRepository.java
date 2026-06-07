package com.spvermicelli.tripledger.billing.domain.request.repository;

import com.spvermicelli.tripledger.billing.domain.request.model.BillChangeRequestAttachment;
import java.util.List;

public interface BillChangeRequestAttachmentRepository {

    void saveAll(Long requestId, List<BillChangeRequestAttachment> attachments);

    List<BillChangeRequestAttachment> findByRequestId(Long requestId);

    void deleteByRequestId(Long requestId);
}
