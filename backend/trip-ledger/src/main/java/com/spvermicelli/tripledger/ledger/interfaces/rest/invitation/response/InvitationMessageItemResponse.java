package com.spvermicelli.tripledger.ledger.interfaces.rest.invitation.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InvitationMessageItemResponse {
    private Long invitationId;
    private Long bookId;
    private String bookName;
    private String bookCoverUrl;
    private String status;
    private LocalDateTime invitedAt;
    private LocalDateTime handledAt;
    private LocalDateTime expireAt;
    private String remark;
    private Long inviterUserId;
    private String inviterNickname;
    private String inviterAvatarUrl;
    private String inviterPhoneNumber;
    private Long inviteeUserId;
    private String inviteeNickname;
    private String inviteeAvatarUrl;
    private String inviteePhoneNumber;
    private boolean canRevoke;
}
