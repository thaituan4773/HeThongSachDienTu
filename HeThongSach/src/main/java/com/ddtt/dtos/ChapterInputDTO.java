package com.ddtt.dtos;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@Data
@Serdeable
@Introspected
public class ChapterInputDTO {
    String title;
    String content;
    Integer coinPrice;
    Integer position;
    String status;
}
