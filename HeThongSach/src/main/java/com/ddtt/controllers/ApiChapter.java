package com.ddtt.controllers;

import com.ddtt.dtos.ChapterContentDTO;
import com.ddtt.services.ChapterService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.security.authentication.Authentication;
import lombok.RequiredArgsConstructor;

@Controller("/api")
@RequiredArgsConstructor
public class ApiChapter {

    private final ChapterService chapterService;

    @Get("/chapters/{chapterId}")
    public HttpResponse<ChapterContentDTO> readChapter(@PathVariable int chapterId, @Nullable Authentication auth) {
        Integer accountId = (auth != null) ? (Integer) auth.getAttributes().get("accountId") : null;
        return HttpResponse.ok(chapterService.readChapter(chapterId, accountId));
    }
    
}
