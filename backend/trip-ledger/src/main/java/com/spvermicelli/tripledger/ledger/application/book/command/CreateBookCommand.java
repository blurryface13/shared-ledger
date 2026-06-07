package com.spvermicelli.tripledger.ledger.application.book.command;

import com.spvermicelli.tripledger.shared.domain.enums.BookType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateBookCommand {
    private Long currentUserId;
    private String name;
    private BookType bookType;
    private String description;
    private String coverUrl;
}
