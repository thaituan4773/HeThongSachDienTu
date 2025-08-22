package com.ddtt.dtos;

import java.util.List;
import lombok.Value;

@Value
public class CategoryDTO {
    private final String id;
    private final String name;
    private final List<BookDTO> books;
}
