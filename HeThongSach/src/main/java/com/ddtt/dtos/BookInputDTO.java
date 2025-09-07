package com.ddtt.dtos;

import io.micronaut.core.annotation.Introspected;
import java.util.List;
import lombok.Data;

@Data
@Introspected
public class BookInputDTO {
    String title;
    String description;
    Integer genreId;
    List<String> tags;
    String coverImageURL;
    String status;
}
