package com.ddtt.controllers;

import com.ddtt.dtos.ChapterContentDTO;
import com.ddtt.dtos.ChapterEditDTO;
import com.ddtt.dtos.ChapterInputDTO;
import com.ddtt.dtos.ChapterOverviewDTO;
import com.ddtt.dtos.ChapterUpdateDTO;
import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.services.ChapterService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Patch;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.security.authentication.Authentication;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Controller("/api")
@RequiredArgsConstructor
public class ApiChapter {

    private final ChapterService chapterService;

    @Get("/chapters/{chapterId}")
    public HttpResponse<ChapterContentDTO> readChapter(@PathVariable int chapterId, Authentication auth) {
        int accountId = (Integer) auth.getAttributes().get("accountId");
        return HttpResponse.ok(chapterService.readChapter(chapterId, accountId));
    }

    @Get("/books/{bookId}/chapters")
    public HttpResponse<PageResponseDTO<ChapterOverviewDTO>> getChaptersInfoPaged(
            @PathVariable int bookId,
            @QueryValue(value = "page", defaultValue = "1") @Min(value = 1, message = "page phải >= 1") int page,
            @QueryValue(value = "desc", defaultValue = "true") boolean desc,
            Authentication authentication
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        return HttpResponse.ok(chapterService.getChaptersInfoPaged(bookId, accountId, page, desc));
    }

    @Post("/chapters/{chapterId}/unlock")
    public HttpResponse unlockChapter(
            @PathVariable int chapterId,
            Authentication authentication
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        chapterService.unlockChapter(accountId, chapterId);
        return HttpResponse.ok();
    }

    @Get("/chapters/{chapterId}/access")
    public HttpResponse<Boolean> checkAccess(
            @PathVariable int chapterId,
            Authentication authentication
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        return HttpResponse.ok(chapterService.checkAccess(chapterId, accountId));
    }

    @Post("/books/{bookId}/chapters")
    public HttpResponse<Boolean> createChapter(
            @PathVariable int bookId,
            Authentication authentication,
            @Body ChapterInputDTO dto
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        return HttpResponse.created(chapterService.addChapter(accountId, bookId, dto));
    }

    @Patch("/chapters/{chapterId}")
    public HttpResponse updateChapter(
            @Body ChapterUpdateDTO dto,
            @PathVariable int chapterId,
            Authentication authentication
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        chapterService.updateChapter(accountId, chapterId, dto);
        return HttpResponse.ok();
    }

    @Get("/chapters/{chapterId}/price")
    public HttpResponse<Integer> getChapterPrice(@PathVariable int chapterId) {
        return HttpResponse.ok(chapterService.getChapterPrice(chapterId));
    }

    @Get("/me/books/{bookId}/chapters")
    public HttpResponse<List<ChapterEditDTO>> gethaptersForEdit(
            @PathVariable int bookId,
            Authentication authentication
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        return HttpResponse.ok(chapterService.getChaptersForEdit(accountId, bookId));
    }

    @Get("/me/chapters/{chapterId}/content")
    public HttpResponse<String> getChatperContentForEdit(
            @PathVariable int chapterId,
            Authentication authentication
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        return HttpResponse.ok(chapterService.getChapterContent(accountId, chapterId));
    }

    @Post("/chapters/{chapterId}/progress")
    public HttpResponse updateProgress(
            @PathVariable int chapterId,
            Authentication authentication,
            @Body("progressPercent") BigDecimal progressPercent
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        chapterService.updateReadingProgress(accountId, chapterId, progressPercent);
        return HttpResponse.ok();
    }

}
