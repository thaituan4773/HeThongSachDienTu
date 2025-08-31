package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Serdeable
public class BookSummaryDTO {
    int bookId;
    String title;
    String coverImageURL;
    long totalView;
    long totalRating;
    double avgRating;
}
