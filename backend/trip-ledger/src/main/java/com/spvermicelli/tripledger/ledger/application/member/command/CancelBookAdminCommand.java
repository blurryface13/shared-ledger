package com.spvermicelli.tripledger.ledger.application.member.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CancelBookAdminCommand {
    private Long currentUserId;
    private Long bookId;
    private Long memberId;
}
