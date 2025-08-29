package com.ddtt.dtos;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Value;

@Value
@Serdeable
@Introspected
public class RatingDTO {
    int bookId;
    @Min(1)
    @Max(5)
    int score;
}
