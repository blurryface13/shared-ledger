package com.spvermicelli.tripledger.ledger.application.tempParticipant.result;

import lombok.Builder;
import lombok.Getter;

/**
 * 临时成员接口中引用的正式成员摘要。
 * 只保留前端展示需要的最小字段，避免直接暴露完整用户或成员模型。
 */
@Getter
@Builder
public class TempParticipantMemberResult {
    private Long memberId;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String phoneNumber;
}
