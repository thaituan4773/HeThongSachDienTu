package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Value;

@Value
@Serdeable
public class RatingDTO {
    int bookId;
    int accountId;
    @Min(1)
    @Max(5)
    int score;
}
