package com.spvermicelli.tripledger.ledger.interfaces.rest.book.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBookRequest {
    @Size(max = 15, message = "name 长度不能超过15个字符")
    private String name;

    @Size(max = 30, message = "description 长度不能超过30个字符")
    private String description;
    private String coverUrl;
}
