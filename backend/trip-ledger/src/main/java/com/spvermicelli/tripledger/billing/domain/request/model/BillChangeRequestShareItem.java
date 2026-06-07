package com.spvermicelli.tripledger.billing.domain.request.model;

import com.spvermicelli.tripledger.shared.domain.enums.ShareMethod;
import com.spvermicelli.tripledger.shared.domain.enums.ShareParticipantType;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BillChangeRequestShareItem {

    private Long id;
    private Long requestId;
    private ShareParticipantType participantType;
    private Long participantRefId;
    private Long attachedMemberId;
    private ShareMethod shareMethod;
    private BigDecimal shareRatio;
    private Long shareAmountCent;
}
