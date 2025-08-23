package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Value;

@Value
@Serdeable
public class GenreDTO {
    private int genre;
    private String genreName;
}
