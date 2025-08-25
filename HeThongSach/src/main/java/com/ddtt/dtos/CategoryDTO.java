package com.ddtt.dtos;

import java.util.List;
import lombok.Value;

@Value
public class CategoryDTO {
    String id;
    String name;
    List<BookDTO> books;
}
