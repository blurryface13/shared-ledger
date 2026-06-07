package com.spvermicelli.tripledger.billing.domain.bill.model;

import com.spvermicelli.tripledger.shared.domain.enums.ShareMethod;
import com.spvermicelli.tripledger.shared.domain.enums.ShareParticipantType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 账单分摊项。
 * 对共享账单而言，真正的消费责任主体落在分摊项上，
 * 因此统计、可见性、结算和支付确认都需要围绕分摊项展开。
 */
@Getter
@Builder
public class BillShareItem {

    private Long id;
    private Long billId;
    private ShareParticipantType participantType;
    private Long participantRefId;
    private Long attachedMemberId;
    private ShareMethod shareMethod;
    private BigDecimal shareRatio;
    private Long shareAmountCent;
    private LocalDateTime createdAt;
}
