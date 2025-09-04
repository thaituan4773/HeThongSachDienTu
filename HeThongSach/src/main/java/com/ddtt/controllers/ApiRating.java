package com.ddtt.controllers;

import com.ddtt.dtos.RatingDTO;
import com.ddtt.services.RatingService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.authentication.Authentication;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller("/api")
@RequiredArgsConstructor
public class ApiRating {

    private final RatingService ratingService;

    @Post("/ratings")
    public HttpResponse<RatingDTO> rateBook(@Body @Valid RatingDTO dto, Authentication authentication) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        return HttpResponse.ok(ratingService.rateBook(dto, accountId));
    }

    @Delete("/ratings/{bookId}")
    public HttpResponse<Void> deleteRating(@PathVariable int bookId, Authentication authentication) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        boolean deleted = ratingService.deleteRating(accountId, bookId);
        
        return deleted ? HttpResponse.noContent() : HttpResponse.notFound();
    }
}
