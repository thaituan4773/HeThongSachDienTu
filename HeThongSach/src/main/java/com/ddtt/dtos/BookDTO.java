package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Value;

@Value
@Serdeable
public class BookDTO {
    int bookId;
    String title;
    String coverImageURL;
}
