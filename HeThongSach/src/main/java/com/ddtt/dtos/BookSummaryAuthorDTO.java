package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Value;

@Value
@Serdeable
public class BookSummaryAuthorDTO {
    int bookId;
    String title;
    String coverImageURL;
    long totalView;
    long totalRating;
    double avgRating;
    int totalDonation;
    String status;
}
