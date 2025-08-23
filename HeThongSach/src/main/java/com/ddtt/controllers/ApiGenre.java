package com.ddtt.controllers;

import com.ddtt.dtos.GenreDTO;
import com.ddtt.services.GenreService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Controller("/api")
@RequiredArgsConstructor
public class ApiGenre {
    private final GenreService genreService;
    
    @Get("/genres")
    public HttpResponse<List<GenreDTO>> getGenres() {
        return HttpResponse.ok(genreService.getAllGenre());
    }
}
