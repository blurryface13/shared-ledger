package com.spvermicelli.tripledger.ledger.application.book.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeleteBookCommand {
    private Long currentUserId;
    private Long bookId;
}
