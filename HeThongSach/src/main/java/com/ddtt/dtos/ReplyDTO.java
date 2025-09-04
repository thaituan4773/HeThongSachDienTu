package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import java.time.OffsetDateTime;
import lombok.Value;


@Value
@Serdeable
public class ReplyDTO {
    int commentId;
    Integer accountId;
    String displayName;
    String avatarUrl;
    String content;
    int score;
    OffsetDateTime createdAt;
    Boolean likedByCurrentUser;
    int parentCommentId;
}
