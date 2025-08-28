package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Serdeable
public class BookSummaryDTO {
    int bookId;
    String title;
    String coverImageURL;
    int totalView;
    int totalRating;
    double avgRating;
}
