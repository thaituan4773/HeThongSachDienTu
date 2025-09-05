package com.ddtt.controllers;

import com.ddtt.dtos.BookFullDetailDTO;
import com.ddtt.dtos.BookSummaryDTO;
import com.ddtt.dtos.CategoryDTO;
import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.services.BookService;
import com.ddtt.services.RecommendationService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.security.authentication.Authentication;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Controller("/api")
@RequiredArgsConstructor
public class ApiBook {

    private final BookService bookService;
    private final RecommendationService recommendationService;
    private final int weekly = 7;

    @Get("/explore")
    public HttpResponse<List<CategoryDTO>> explore(Authentication authentication) {
        List<CategoryDTO> categories = new ArrayList<>();
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        categories.add(recommendationService.recommendBooksForUserWithTags(accountId));
        categories.add(bookService.findTrendingBooks(weekly));
        categories.add(bookService.findTopRatedBooks());
        categories.add(bookService.findNewestBooks());
        categories.addAll(bookService.randomGenreCategories(2));
        return HttpResponse.ok(categories);
    }

    @Get("/books/{bookId}")
    public HttpResponse<BookFullDetailDTO> viewBookDetail(@PathVariable int bookId, Authentication authentication) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        BookFullDetailDTO bookDetail = bookService.getBookDetail(bookId, accountId);
        if (bookDetail == null) {
            return HttpResponse.notFound();
        }
        return HttpResponse.ok(bookDetail);
    }

    @Get("/books/search")
    public PageResponseDTO<BookSummaryDTO> searchBooks(
            @Nullable @QueryValue("kw") @Size(max = 200, message = "Keyword quá dài (tối đa 200 ký tự)") String kw,
            @QueryValue(value = "page", defaultValue = "1") @Min(value = 1, message = "page phải >= 1") int page
    ) {
        return bookService.searchBooks(kw, page);
    }

    @Get("/genre/{genreId}/books")
    public HttpResponse<PageResponseDTO<BookSummaryDTO>> getBookByGenre(
            @PathVariable int genreId,
            @Nullable @QueryValue("sortMode") String sort, // "trending", "views", "topRated", "newest"
            @QueryValue(value = "page", defaultValue = "1") @Min(value = 1, message = "page phải >= 1") int page
    ) {
        return HttpResponse.ok(bookService.findBooksByGenrePaged(genreId, page, sort));
    }
}
