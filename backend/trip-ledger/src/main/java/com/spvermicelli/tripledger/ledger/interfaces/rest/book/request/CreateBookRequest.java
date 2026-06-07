package com.spvermicelli.tripledger.ledger.interfaces.rest.book.request;

import com.spvermicelli.tripledger.shared.domain.enums.BookType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBookRequest {
    @NotBlank(message = "name 不能为空")
    @Size(max = 15, message = "name 长度不能超过15个字符")
    private String name;

    @NotNull(message = "bookType 不能为空")
    private BookType bookType;

    @Size(max = 30, message = "description 长度不能超过30个字符")
    private String description;
    private String coverUrl;
}
