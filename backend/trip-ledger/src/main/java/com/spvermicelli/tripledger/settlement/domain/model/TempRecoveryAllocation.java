package com.spvermicelli.tripledger.settlement.domain.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 临时成员回补账单分配记录。
 * 一条临时成员回补可能同时覆盖多笔历史帮带/共享分摊，
 * 因此需要单独落表记录每笔账单被回补了多少金额。
 */
@Getter
@Builder
public class TempRecoveryAllocation {

    private Long id;
    private Long recoveryRecordId;
    private Long billId;
    private Long allocatedAmountCent;
    private LocalDateTime createdAt;
}
