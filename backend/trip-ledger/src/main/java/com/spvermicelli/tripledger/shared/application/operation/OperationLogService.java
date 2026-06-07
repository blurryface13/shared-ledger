package com.spvermicelli.tripledger.shared.application.operation;

import com.spvermicelli.tripledger.shared.domain.enums.OperationTargetType;
import com.spvermicelli.tripledger.shared.domain.enums.OperationType;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.mapper.OperationLogMapper;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.OperationLogPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OperationLogService {

    private static final int MAX_TEXT_LENGTH = 2000;

    private final OperationLogMapper operationLogMapper;

    public OperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void log(
        Long bookId,
        Long operatorUserId,
        Long operatorMemberId,
        OperationType operationType,
        OperationTargetType targetType,
        Long targetId,
        String beforeSummary,
        String afterSummary
    ) {
        if (operationType == null || targetType == null || targetId == null) {
            return;
        }
        OperationLogPO po = new OperationLogPO();
        po.setBookId(bookId);
        po.setOperatorUserId(operatorUserId);
        po.setOperatorMemberId(operatorMemberId);
        po.setOperationType(operationType);
        po.setTargetType(targetType);
        po.setTargetId(targetId);
        po.setBeforeJson(trimToNull(beforeSummary));
        po.setAfterJson(trimToNull(afterSummary));
        operationLogMapper.insert(po);
    }

    private String trimToNull(String rawText) {
        if (!StringUtils.hasText(rawText)) {
            return null;
        }
        String text = rawText.trim();
        if (text.length() <= MAX_TEXT_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_TEXT_LENGTH);
    }
}
