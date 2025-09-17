package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import java.time.OffsetDateTime;
import lombok.Value;

@Value  
@Serdeable
public class CurrentReadingDTO {
    int bookId;
    String title;
    String coverImageURL;
    int currentChapterId;
    String chapterTitle;
    OffsetDateTime lastUpdatedAt;
}
