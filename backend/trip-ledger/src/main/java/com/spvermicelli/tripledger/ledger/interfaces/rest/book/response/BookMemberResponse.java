package com.spvermicelli.tripledger.ledger.interfaces.rest.book.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookMemberResponse {
    private Long memberId;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String phoneNumber;
    private String memberRole;
    private String memberStatus;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
}
