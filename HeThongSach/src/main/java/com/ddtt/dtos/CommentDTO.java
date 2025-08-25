package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import java.time.OffsetDateTime;
import lombok.Value;

@Value
@Serdeable
public class CommentDTO {
    int commentId;
    Integer accountId;
    String displayName;
    String avatarUrl;
    String content;
    int likesCount;
    OffsetDateTime createdAt;
    int replyCount;
    Boolean likedByCurrentUser; // true = like, false = dislike, null = chưa tương tá
}
