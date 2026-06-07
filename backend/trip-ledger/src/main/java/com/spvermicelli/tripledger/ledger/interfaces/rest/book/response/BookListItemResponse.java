package com.spvermicelli.tripledger.ledger.interfaces.rest.book.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookListItemResponse {
    private Long bookId;
    private String name;
    private String bookType;
    private String description;
    private String coverUrl;
    private LocalDateTime createdAt;
}
