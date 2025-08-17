package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import lombok.Value;

@Value
@Serdeable
public class BookSectionDTO {
    String title;
    List<BookDTO> books;
}
