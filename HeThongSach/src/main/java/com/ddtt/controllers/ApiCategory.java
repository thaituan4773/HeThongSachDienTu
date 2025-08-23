package com.ddtt.controllers;

import com.ddtt.dtos.BookDTO;
import com.ddtt.dtos.CategoryDTO;
import com.ddtt.services.AccountService;
import com.ddtt.services.BookService;
import com.ddtt.services.RecommendationService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.authentication.Authentication;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Controller("/api")
@RequiredArgsConstructor
public class ApiCategory {

    private final BookService bookService;
    private final RecommendationService recommendationService;
    private final int weekly = 7;

    @Get("/explore")
    public HttpResponse<List<CategoryDTO>> getCates(@Nullable Authentication authentication) {
        List<CategoryDTO> categories = new ArrayList<>();
        if (authentication != null) {
            Integer accountId = (Integer) authentication.getAttributes().get("accountId");
            categories.add(recommendationService.recommendBooksForUserWithTags(accountId));
        }
        categories.add(bookService.findTrendingBooks(weekly));
        categories.add(bookService.findTopRatedBooks());
        categories.add(bookService.findNewestBooks());
        categories.addAll(bookService.randomGenreCategories(2));
        return HttpResponse.ok(categories);
    }
    
    
}
