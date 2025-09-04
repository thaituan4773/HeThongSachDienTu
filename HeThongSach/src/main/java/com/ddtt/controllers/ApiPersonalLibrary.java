package com.ddtt.controllers;

import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.dtos.PersonalLibraryBookDTO;
import com.ddtt.services.PersonalLibraryService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.security.authentication.Authentication;
import lombok.RequiredArgsConstructor;

@Controller("/api")
@RequiredArgsConstructor
public class ApiPersonalLibrary {

    private final PersonalLibraryService personalLibraryService;

    @Get("/library")
    public HttpResponse<PageResponseDTO<PersonalLibraryBookDTO>> getPersonalLibraryBooks(
            Authentication authentication,
            @Nullable @QueryValue(value = "kw") String kw,
            @QueryValue(value = "page", defaultValue = "1") int page,
            @QueryValue(value = "sortBy", defaultValue = "title") String sortBy, // title, unreadChapters, totalChapters, lastReadAt, addedAt
            @QueryValue(value = "desc", defaultValue = "true") boolean desc, // desc | asc
            @QueryValue(value = "unreaded") @Nullable Boolean unreaded // null = tất cả, true = unread, false = read

    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        PageResponseDTO<PersonalLibraryBookDTO> result = personalLibraryService
                .getPersonalLibrary(accountId, kw, page, sortBy, desc, unreaded);

        return HttpResponse.ok(result);
    }

    @Post("/library/{bookId}")
    public HttpResponse<String> addBook(@PathVariable int bookId, Authentication authentication) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        personalLibraryService.addBookToLibrary(accountId, bookId);
            return HttpResponse.ok("Sách thêm thành công");
    }
    
    @Delete("/library/{bookId}")
    public HttpResponse<Void> deleteBook(@PathVariable int bookId, Authentication authentication) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        boolean deleted = personalLibraryService.deleteBookFromLibrary(accountId, bookId);
        return deleted ? HttpResponse.noContent() : HttpResponse.notFound();
    }
}
