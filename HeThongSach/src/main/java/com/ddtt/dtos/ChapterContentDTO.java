package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import java.math.BigDecimal;
import lombok.Value;

@Value
@Serdeable
public class ChapterContentDTO {
    int chapterId;
    String title;
    String content;
    Integer prevChapterId;
    Integer nextChapterId;
    int commentCount;
    BigDecimal progressPercent; 
}
