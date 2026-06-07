package com.spvermicelli.tripledger.billing.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spvermicelli.tripledger.billing.domain.bill.model.BillAttachment;
import com.spvermicelli.tripledger.billing.domain.bill.repository.BillAttachmentRepository;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.mapper.BillAttachmentMapper;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.po.BillAttachmentPO;
import com.spvermicelli.tripledger.shared.domain.enums.AttachmentFileType;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class BillAttachmentRepositoryImpl implements BillAttachmentRepository {

    private final BillAttachmentMapper billAttachmentMapper;

    public BillAttachmentRepositoryImpl(BillAttachmentMapper billAttachmentMapper) {
        this.billAttachmentMapper = billAttachmentMapper;
    }

    @Override
    public List<BillAttachment> findByBillId(Long billId) {
        if (billId == null) {
            return List.of();
        }
        return billAttachmentMapper.selectList(new LambdaQueryWrapper<BillAttachmentPO>()
                .eq(BillAttachmentPO::getBillId, billId)
                .orderByAsc(BillAttachmentPO::getId))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public void replaceAll(Long billId, Long uploadedByMemberId, List<String> attachmentUrls) {
        if (billId == null) {
            return;
        }
        billAttachmentMapper.delete(new LambdaQueryWrapper<BillAttachmentPO>()
            .eq(BillAttachmentPO::getBillId, billId));
        if (attachmentUrls == null || attachmentUrls.isEmpty() || uploadedByMemberId == null) {
            return;
        }
        Set<String> dedupUrls = new LinkedHashSet<>();
        attachmentUrls.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .filter(url -> dedupUrls.add(url))
            .forEach(url -> {
                BillAttachmentPO po = new BillAttachmentPO();
                po.setBillId(billId);
                po.setFileUrl(url);
                po.setFileType(resolveFileType(url));
                po.setUploadedByMemberId(uploadedByMemberId);
                billAttachmentMapper.insert(po);
            });
    }

    private BillAttachment toDomain(BillAttachmentPO po) {
        if (po == null) {
            return null;
        }
        return BillAttachment.builder()
            .id(po.getId())
            .billId(po.getBillId())
            .fileUrl(po.getFileUrl())
            .fileType(po.getFileType())
            .uploadedByMemberId(po.getUploadedByMemberId())
            .createdAt(po.getCreatedAt())
            .build();
    }

    private AttachmentFileType resolveFileType(String url) {
        String safeUrl = Objects.toString(url, "").toLowerCase();
        if (safeUrl.endsWith(".jpg")
            || safeUrl.endsWith(".jpeg")
            || safeUrl.endsWith(".png")
            || safeUrl.endsWith(".gif")
            || safeUrl.endsWith(".webp")
            || safeUrl.contains("image")) {
            return AttachmentFileType.IMAGE;
        }
        return AttachmentFileType.OTHER;
    }
}
