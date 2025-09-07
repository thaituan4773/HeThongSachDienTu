package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import lombok.Data;

@Data
@Serdeable
public class BookFullDetailDTO {
    String bookName;
    String genre;
    String coverImageURL;
    String authorName;
    int authorId;
    String status;
    String description;
    int totalView;
    int totalRating;
    double avgRating;
    Integer totalDonate; // ẩn nếu user ko phải author
    boolean isAuthor;
    boolean isInLibrary;
    int userScore;
    List<TagDTO> tags;
}
