package com.ddtt.dtos;

import java.util.Date;
import lombok.Value;

@Value
public class ChapterDTO {
    private int chapterId;
    private String title;
    private int order;
    private int coinPrice;
    private Date createdDate;
}
