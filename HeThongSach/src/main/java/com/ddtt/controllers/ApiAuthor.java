package com.ddtt.controllers;

import com.ddtt.dtos.BookCreateDTO;
import com.ddtt.dtos.BookSummaryDTO;
import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.services.BookService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Part;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.security.authentication.Authentication;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Controller("/api")
@RequiredArgsConstructor
public class ApiAuthor {

    private final BookService bookService;

    @Get("/me/books")
    public HttpResponse<PageResponseDTO<BookSummaryDTO>> getMyBooks(
            @QueryValue(value = "page", defaultValue = "1") @Min(value = 1, message = "page phải >= 1") int page,
            Authentication authentication
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        return HttpResponse.ok(bookService.findBooksByAuthorPaged(accountId, page, true));
    }

    @Post(value = "/me/books", consumes = "multipart/form-data")
    public HttpResponse<BookSummaryDTO> addBook(
            @Part("title") @NotBlank(message = "Yêu cầu tựa đề") String title,
            @Nullable @Part("description") String description,
            @Part("genreId") @Min(value = 1, message = "Yêu cầu chọn thể loại") int genreId,
            @Nullable @Part("tagIds") List<Integer> tagIds,
            @Nullable @Part(value = "coverImage") CompletedFileUpload file,
            Authentication authentication
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        BookCreateDTO book = new BookCreateDTO();
        book.setTitle(title);
        book.setDescription(description);
        book.setGenreId(genreId);
        book.setTagIds(tagIds);
        return HttpResponse.ok(bookService.createBook(book, accountId, file));
    }

}
