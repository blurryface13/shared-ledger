package com.spvermicelli.tripledger.ledger.application.book.result;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookDetailResult {
    private Long bookId;
    private String name;
    private String bookType;
    private String description;
    private String coverUrl;
    private Long ownerUserId;
    private Long currentMemberId;
    private String currentMemberRole;
    private String currentMemberStatus;
    private boolean shared;
    private boolean canEditBook;
    private boolean canInviteMember;
    private boolean canRemoveMember;
    private boolean canCancelAdmin;
    private boolean canTransferOwner;
    private boolean canQuitBook;
    private boolean canDeleteBook;
    private List<BookMemberResult> members;
    private List<TempParticipantResult> tempParticipants;
}
