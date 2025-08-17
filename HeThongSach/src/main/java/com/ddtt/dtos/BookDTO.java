package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import lombok.Value;


@Value
@Serdeable
public class BookDTO {
    int bookID;
    String title;
    String coverImageURL;
    List<String> categoryName;
}
