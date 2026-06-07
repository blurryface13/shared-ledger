package com.spvermicelli.tripledger.ledger.interfaces.rest.member.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferOwnerRequest {
    private Long targetMemberId;
}
