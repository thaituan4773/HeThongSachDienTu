package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Value;

@Value
@Serdeable
public class BookSummaryDTO {
    private final int bookId;
    private final String title;
    private final String coverImageURL;
    private final int totalView;
    private final int totalRating;
    private final double avgRating;
}
