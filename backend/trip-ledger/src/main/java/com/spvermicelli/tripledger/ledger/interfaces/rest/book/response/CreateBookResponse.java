package com.spvermicelli.tripledger.ledger.interfaces.rest.book.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateBookResponse {
    private Long bookId;
}
