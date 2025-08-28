package com.ddtt.dtos;

import java.util.List;
import lombok.Data;

@Data
public class BookCreateDTO {
    String title; // not null
    String description;
    int genreId; // not null
    List<Integer> tagIds;
    String coverImageURL;
    
}
