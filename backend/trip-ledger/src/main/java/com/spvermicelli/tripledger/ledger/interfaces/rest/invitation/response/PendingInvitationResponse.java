package com.spvermicelli.tripledger.ledger.interfaces.rest.invitation.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PendingInvitationResponse {
    private Long invitationId;
    private Long bookId;
    private String bookName;
    private Long inviterUserId;
    private String inviterNickname;
    private String inviterAvatarUrl;
    private String status;
    private LocalDateTime invitedAt;
    private LocalDateTime expireAt;
    private String remark;
}
