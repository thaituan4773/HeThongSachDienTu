package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import java.util.Date;
import lombok.Value;

@Value
@Serdeable
public class ChapterEditDTO {
    int chapterId;
    String title;
    int position;
    int coinPrice;
    Date createdDate;
    String status;
}
