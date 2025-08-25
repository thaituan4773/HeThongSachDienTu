package com.ddtt.controllers;

import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.dtos.PersonalLibraryBookDTO;
import com.ddtt.services.PersonalLibraryService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
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
            @QueryValue(value = "page", defaultValue = "1") int page,
            @QueryValue(value = "sortBy", defaultValue = "title") String sortBy, // title, unreadChapters, totalChapters, lastReadAt, addedAt
            @QueryValue(value = "desc", defaultValue = "true") boolean desc, // desc | asc
            @QueryValue(value = "unreaded") @Nullable Boolean unreaded // true = chỉ sách còn chương chưa đọc, false = chỉ sách đọc hết

    ) {
        Integer accountId = (Integer) authentication.getAttributes().get("accountId");
        PageResponseDTO<PersonalLibraryBookDTO> result = personalLibraryService
                .getPersonalLibrary(accountId, page, sortBy, desc, unreaded);

        return HttpResponse.ok(result);
    }
}
