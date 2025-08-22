package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Value;

@Value
@Serdeable
public class BookDTO {
    private int bookId;
    private String title;
    private String coverImageURL;
}
