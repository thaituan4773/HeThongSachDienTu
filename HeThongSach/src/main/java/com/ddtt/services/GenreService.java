package com.ddtt.services;

import com.ddtt.dtos.GenreDTO;
import com.ddtt.repositories.GenreRepository;
import jakarta.inject.Singleton;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class GenreService {
    private final GenreRepository genreRepository;
    
    public List<GenreDTO> getAllGenre(){
        return genreRepository.getAllGenre();
    }
}
