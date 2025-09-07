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
    private final int limit = 6;
    
    public List<TagDTO> getAllTags(){
        return tagRepository.getAllTags();
    }
    
    public List<String> suggestTagNames(String prefix){
        return tagRepository.suggestTagNames(prefix, limit);
    }
}
