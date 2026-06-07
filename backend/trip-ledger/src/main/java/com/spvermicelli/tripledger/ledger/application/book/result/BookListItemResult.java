package com.spvermicelli.tripledger.ledger.application.book.result;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookListItemResult {
    private Long bookId;
    private String name;
    private String bookType;
    private String description;
    private String coverUrl;
    private LocalDateTime createdAt;
}
