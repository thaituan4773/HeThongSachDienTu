package com.ddtt.dtos;

import lombok.Value;

@Value
public class ChapterAccessDTO {
    int chapterId;
    int coinPrice;
    boolean alreadyPurchased;
}
