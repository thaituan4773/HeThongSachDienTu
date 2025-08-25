package com.ddtt.dtos;

import lombok.Data;

@Data
public class ChapterCreateDTO {
    int bookId;
    int position;
    String title;
    String content;
    int coinPrice;
    String status;
}
