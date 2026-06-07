package com.spvermicelli.tripledger.billing.domain.request.model;

import com.spvermicelli.tripledger.shared.domain.enums.ApprovalAction;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 账单申请审批动作记录。
 * 该对象只记录“谁在什么时候做了什么动作”，不承担待办任务的职责。
 */
@Getter
@Builder
public class BillChangeRequestApproval {

    private Long id;
    private Long requestId;
    private Long approverMemberId;
    private ApprovalAction approvalAction;
    private String approvalComment;
    private LocalDateTime createdAt;
}
