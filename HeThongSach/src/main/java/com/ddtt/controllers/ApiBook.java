package com.ddtt.controllers;

import com.ddtt.dtos.BookFullDetailDTO;
import com.ddtt.dtos.BookInputDTO;
import com.ddtt.dtos.BookSummaryDTO;
import com.ddtt.dtos.CategoryDTO;
import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.services.BookService;
import com.ddtt.services.RecommendationService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Part;
import io.micronaut.http.annotation.Patch;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.multipart.CompletedFileUpload;
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
        categories.add(recommendationService.recommendBooksForUser(accountId));
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
    public HttpResponse<PageResponseDTO<BookSummaryDTO>> searchBooks(
            @Nullable @QueryValue("titleKw") @Size(max = 300) String titleKw,
            @Nullable @QueryValue("descKw") @Size(max = 500) String descKw,
            @Nullable @QueryValue("authorName") @Size(max = 100) String authorName,
            @Nullable @QueryValue("tags") List<String> tags,
            @Nullable @QueryValue("genreId") Integer genreId,
            @QueryValue(value = "page", defaultValue = "1") @Min(1) int page,
            @Nullable @QueryValue("sort") String sort
    ) {
        PageResponseDTO<BookSummaryDTO> result = bookService.searchOrFilterBooks(
                titleKw,
                descKw,
                authorName,
                tags,
                genreId,
                page,
                sort
        );
        return HttpResponse.ok(result);
    }

    @Patch(value = "/books/{bookId}", consumes = "multipart/form-data")
    public HttpResponse<Boolean> updateBook(
            @PathVariable int bookId,
            @Nullable @Part("title") String title,
            @Nullable @Part("description") String description,
            @Nullable @Part("genreId") Integer genreId,
            @Nullable @Part("tags") List<String> tags,
            @Nullable @Part("coverImage") CompletedFileUpload coverImage,
            @Nullable @Part("status") String status,
            Authentication authentication
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        BookInputDTO dto = new BookInputDTO();
        dto.setTitle(title);
        dto.setDescription(description);
        dto.setGenreId(genreId);
        dto.setTags(tags);
        dto.setStatus(status);
        return HttpResponse.ok(bookService.updateBook(bookId, dto, accountId, coverImage));
    }

    @Delete("/books/{bookId}")
    public HttpResponse deleteBook(
            @PathVariable int bookId,
            Authentication authentication
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        bookService.softDeleteBook(bookId, accountId);
        return HttpResponse.noContent();
    }
}
