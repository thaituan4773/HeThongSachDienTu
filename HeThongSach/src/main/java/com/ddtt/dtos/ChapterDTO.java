package com.ddtt.dtos;

import java.util.Date;
import lombok.Value;

@Value
public class ChapterDTO {
    int chapterId;
    String title;
    int order;
    int coinPrice;
    Date createdDate;
}
