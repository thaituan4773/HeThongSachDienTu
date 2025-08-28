package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import java.util.Date;
import lombok.Value;

@Value
@Serdeable
public class ChapterOverviewDTO {
    int chapterId;
    String title;
    int order;
    int coinPrice;
    Date createdDate;
    boolean hasRead;
    boolean hasUnlocked;
}
