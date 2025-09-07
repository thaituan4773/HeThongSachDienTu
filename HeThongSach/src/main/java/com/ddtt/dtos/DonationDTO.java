package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import java.time.OffsetDateTime;
import lombok.Value;

@Value
@Serdeable
public class DonationDTO {
    String bookCover;
    String bookName;
    int amount;
    OffsetDateTime createdAt;
}
