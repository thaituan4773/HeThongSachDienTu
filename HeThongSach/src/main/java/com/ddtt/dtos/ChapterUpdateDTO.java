package com.ddtt.dtos;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@Data
@Introspected
@Serdeable
public class ChapterUpdateDTO {
    String title;
    Integer coinPrice;
    String status;
    String content;
}
