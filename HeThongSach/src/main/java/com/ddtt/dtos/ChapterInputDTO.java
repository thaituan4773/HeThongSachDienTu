package com.ddtt.dtos;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class ChapterInputDTO {
    Integer chapterId;
    int position;
    String title;
    String content;
    int coinPrice;
    String status;
    OffsetDateTime createdAt;
}
