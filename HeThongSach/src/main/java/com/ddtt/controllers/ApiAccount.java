package com.ddtt.controllers;

import com.ddtt.dtos.AccountDTO;
import com.ddtt.dtos.BookInputDTO;
import com.ddtt.dtos.BookSummaryAuthorDTO;
import com.ddtt.dtos.BookSummaryDTO;
import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.services.AccountService;
import com.ddtt.services.BookService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Part;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.security.authentication.Authentication;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Controller("/api")
@RequiredArgsConstructor
public class ApiAccount {

    private final BookService bookService;
    private final AccountService accountService;

    @Get("/me/books")
    public HttpResponse<PageResponseDTO<BookSummaryAuthorDTO>> getMyBooks(
            @QueryValue(value = "page", defaultValue = "1") @Min(value = 1, message = "page phải >= 1") int page,
            Authentication authentication
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        return HttpResponse.ok(bookService.getMyBooks(accountId, page));
    }

    @Post(value = "/me/books", consumes = "multipart/form-data")
    public HttpResponse<BookSummaryAuthorDTO> addBook(
            @Part("title") @NotBlank(message = "Yêu cầu tựa đề") String title,
            @Nullable @Part("description") String description,
            @Part("genreId") @Min(value = 1, message = "Yêu cầu chọn thể loại") int genreId,
            @Nullable @Size(max = 10) @Part("tags") List<String> tags,
            @Nullable @Part("coverImage") CompletedFileUpload file,
            Authentication authentication
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        BookInputDTO book = new BookInputDTO();
        book.setTitle(title);
        book.setDescription(description);
        book.setGenreId(genreId);
        book.setTags(tags);
        book.setStatus("DRAFT");
        return HttpResponse.ok(bookService.createBook(book, accountId, file));
    }

    @Get("/users/{accountId}")
    public HttpResponse<AccountDTO> getProfile(@PathVariable int accountId) {
        return HttpResponse.ok(accountService.getAccountById(accountId));
    }

    @Get("/me")
    public HttpResponse<AccountDTO> getMyProfile(Authentication authentication) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        return HttpResponse.ok(accountService.getAccountById(accountId));
    }

    @Get("/me/balance")
    public HttpResponse<Integer> getBalance(Authentication authentication) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        return HttpResponse.ok(accountService.getBalance(accountId));
    }

    @Get("/users/{authorId}/books")
    public HttpResponse<PageResponseDTO<BookSummaryDTO>> getBooksByUser(
            @QueryValue(value = "page", defaultValue = "1") @Min(value = 1, message = "page phải >= 1") int page,
            @PathVariable int authorId
    ) {
        return HttpResponse.ok(bookService.findBooksByAuthorPaged(authorId, page));
    }

}
