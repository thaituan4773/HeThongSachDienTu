package com.ddtt.controllers;

import com.ddtt.dtos.CommentDTO;
import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.dtos.ReplyDTO;
import com.ddtt.services.CommentService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.security.authentication.Authentication;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Controller("/api")
@RequiredArgsConstructor
public class ApiComment {
    
    private final CommentService commentService;
    
    @Get("/chapters/{chapterId}/comments")
    public HttpResponse<PageResponseDTO<CommentDTO>> viewComments(
            @PathVariable int chapterId,
            Authentication authentication,
            @QueryValue(value = "page", defaultValue = "1") @Min(value = 1, message = "page phải >= 1")
            int page,
            @QueryValue(value = "sort", defaultValue = "")
            String sort
    ){
        Integer userId = (Integer) authentication.getAttributes().get("accountId");
        return HttpResponse.ok(commentService.getCommentsByChapter(chapterId, page, userId, sort));
    }
    
    @Get("/comments/{commentId}")
    public HttpResponse<PageResponseDTO<ReplyDTO>> viewReply(
            @PathVariable int commentId,
            Authentication authentication,
            @QueryValue(value = "page", defaultValue = "1") @Min(value = 1, message = "page phải >= 1")
            int page
    ) {
        Integer userId = (Integer) authentication.getAttributes().get("accountId");
        return HttpResponse.ok(commentService.getRepliesByComment(commentId, page, userId));
    }
    
}
