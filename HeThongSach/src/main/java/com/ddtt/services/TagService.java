package com.ddtt.services;

import com.ddtt.dtos.TagDTO;
import com.ddtt.repositories.TagRepository;
import jakarta.inject.Singleton;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;
    
    public List<TagDTO> getAllTags(){
        return tagRepository.getAllTags();
    }
}
