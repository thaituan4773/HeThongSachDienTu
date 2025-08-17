package com.ddtt.dtos;

import java.util.Date;
import lombok.Value;

@Value
public class ChapterDTO {
    int chapterID;
    String title;
    int totalLike;
    int order;
    Date createdDate;
}
