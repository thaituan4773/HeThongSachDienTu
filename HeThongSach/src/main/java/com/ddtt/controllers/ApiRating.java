package com.ddtt.controllers;

import com.ddtt.dtos.RatingDTO;
import com.ddtt.services.RatingService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller("/api")
@RequiredArgsConstructor
public class ApiRating {

    private final RatingService ratingService;

    @Post("/ratings")
    public HttpResponse<RatingDTO> rateBook(@Body @Valid RatingDTO dto) {
        RatingDTO rating = ratingService.rateBook(dto);
        return HttpResponse.ok(rating);
    }
}
