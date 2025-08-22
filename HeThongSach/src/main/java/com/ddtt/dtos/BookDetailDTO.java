package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import lombok.Data;

@Data
@Serdeable
public class BookDetailDTO {
    String bookName;
    String genre;
    Integer authorID;
    String description;
    int totalView;
    int totalRating;
    double avgRating;
    int totalDonate;
    List<ChapterDTO> chapters;
}
