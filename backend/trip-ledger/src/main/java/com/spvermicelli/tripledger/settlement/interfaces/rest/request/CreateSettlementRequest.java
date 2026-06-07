package com.spvermicelli.tripledger.settlement.interfaces.rest.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSettlementRequest {
    private String strategyType;
}
