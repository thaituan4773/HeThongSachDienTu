package com.ddtt.dtos;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import lombok.Data;

@Data
@Serdeable
@Introspected
public class BookInputDTO {
    String title;
    String description;
    Integer genreId;
    List<String> tags;
    String coverImageURL;
    String status;
}
