package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import lombok.Data;

@Data
@Serdeable
public class BookDetailDTO {
    private String bookName;
    private String genre;
    private Integer authorID;
    private String description;
    private int totalView;
    private int totalRating;
    private double avgRating;
    private int totalDonate;
    private List<ChapterDTO> chapters;
}
