package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import java.time.OffsetDateTime;
import lombok.Value;

@Value
@Serdeable
public class PersonalLibraryBookDTO {
    int bookId;
    String title;
    String coverImageURL;
    int totalChapters;
    int unreadChapters;
    OffsetDateTime addedAt;
    OffsetDateTime lastReadAt;
}