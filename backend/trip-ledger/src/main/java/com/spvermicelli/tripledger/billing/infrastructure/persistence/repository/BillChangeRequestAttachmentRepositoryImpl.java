package com.spvermicelli.tripledger.billing.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spvermicelli.tripledger.billing.domain.request.model.BillChangeRequestAttachment;
import com.spvermicelli.tripledger.billing.domain.request.repository.BillChangeRequestAttachmentRepository;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.mapper.BillChangeRequestAttachmentMapper;
import com.spvermicelli.tripledger.billing.infrastructure.persistence.po.BillChangeRequestAttachmentPO;
import com.spvermicelli.tripledger.shared.domain.enums.AttachmentFileType;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class BillChangeRequestAttachmentRepositoryImpl implements BillChangeRequestAttachmentRepository {

    private final BillChangeRequestAttachmentMapper mapper;

    public BillChangeRequestAttachmentRepositoryImpl(BillChangeRequestAttachmentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void saveAll(Long requestId, List<BillChangeRequestAttachment> attachments) {
        deleteByRequestId(requestId);
        if (requestId == null || attachments == null || attachments.isEmpty()) {
            return;
        }
        Set<String> dedup = new LinkedHashSet<>();
        attachments.stream()
            .filter(item -> item != null && StringUtils.hasText(item.getFileUrl()))
            .filter(item -> dedup.add(item.getFileUrl().trim()))
            .forEach(item -> {
                BillChangeRequestAttachmentPO po = new BillChangeRequestAttachmentPO();
                po.setRequestId(requestId);
                po.setFileUrl(item.getFileUrl().trim());
                po.setFileType(item.getFileType() == null ? resolveFileType(item.getFileUrl()) : item.getFileType());
                po.setUploadedByMemberId(item.getUploadedByMemberId());
                mapper.insert(po);
            });
    }

    @Override
    public List<BillChangeRequestAttachment> findByRequestId(Long requestId) {
        if (requestId == null) {
            return List.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<BillChangeRequestAttachmentPO>()
                .eq(BillChangeRequestAttachmentPO::getRequestId, requestId)
                .orderByAsc(BillChangeRequestAttachmentPO::getId))
            .stream()
            .map(po -> BillChangeRequestAttachment.builder()
                .id(po.getId())
                .requestId(po.getRequestId())
                .fileUrl(po.getFileUrl())
                .fileType(po.getFileType())
                .uploadedByMemberId(po.getUploadedByMemberId())
                .createdAt(po.getCreatedAt())
                .build())
            .toList();
    }

    @Override
    public void deleteByRequestId(Long requestId) {
        if (requestId == null) {
            return;
        }
        mapper.delete(new LambdaQueryWrapper<BillChangeRequestAttachmentPO>()
            .eq(BillChangeRequestAttachmentPO::getRequestId, requestId));
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
