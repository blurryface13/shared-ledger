package com.spvermicelli.tripledger.billing.interfaces.rest.statistics.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberRelationResponse {
    private String targetParticipantType;
    private Long targetParticipantId;
    private Long targetMemberId;
    private String targetMemberName;
    private String targetMemberAvatarUrl;
    private Long iOweTargetAmountCent;
    private Long iPaidTargetAmountCent;
    private Long iStillNeedPayTargetAmountCent;
    private Long iOweTargetTempAmountCent;
    private Long targetOwesMeAmountCent;
    private Long targetPaidMeAmountCent;
    private Long targetStillNeedPayMeAmountCent;
    private Long targetOwesMeTempAmountCent;
    private String netDirection;
    private Long netAmountCent;
}
