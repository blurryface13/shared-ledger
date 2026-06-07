package com.spvermicelli.tripledger.ledger.application.book.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateBookCommand {
    private Long currentUserId;
    private Long bookId;
    private String name;
    private String description;
    private String coverUrl;
}
