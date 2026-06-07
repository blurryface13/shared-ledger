package com.spvermicelli.tripledger.ledger.application.book.result;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateBookResult {
    private Long bookId;
}
