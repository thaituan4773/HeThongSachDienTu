package com.ddtt.dtos;

import java.util.List;
import lombok.Value;

@Value
public class BookDetailDTO {
    String bookName;
    List<String> categoryName;
    Integer authorID;
    String description;
    int totalView;
    int totalLike;
    int totalDonate;
    List<ChapterDTO> chapters;
}
