package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import lombok.Value;


@Value
@Serdeable
public class PageResponseDTO<T> {
    private final long total;      // tổng items
    private final int page;        // current page
    private final int size;
    private final int totalPages; 
    private final List<T> items;
}
