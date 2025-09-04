package com.ddtt.controllers;

import com.ddtt.dtos.CommentDTO;
import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.dtos.ReplyDTO;
import com.ddtt.services.CommentService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.security.authentication.Authentication;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;

@Controller("/api")
@RequiredArgsConstructor
public class ApiComment {

    private final CommentService commentService;

    @Get("/chapters/{chapterId}/comments")
    public HttpResponse<PageResponseDTO<CommentDTO>> viewComments(
            @PathVariable(value = "chapterId") int chapterId,
            Authentication authentication,
            @QueryValue(value = "page", defaultValue = "1") @Min(value = 1, message = "page phải >= 1") int page,
            @QueryValue(value = "sort", defaultValue = "") String sort // mostliked, newest
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        return HttpResponse.ok(commentService.getCommentsByChapter(chapterId, page, accountId, sort));
    }

    @Get("/comments/{commentId}/replies")
    public HttpResponse<PageResponseDTO<ReplyDTO>> viewReply(
            @PathVariable int commentId,
            Authentication authentication,
            @QueryValue(value = "page", defaultValue = "1") @Min(value = 1, message = "page phải >= 1") int page
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        return HttpResponse.ok(commentService.getRepliesByComment(commentId, page, accountId));
    }

    @Post("/chapters/{chapterId}/comments")
    public HttpResponse<CommentDTO> addComment(
            @PathVariable int chapterId,
            Authentication authentication,
            @Body("content") @NotEmpty String content
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        return HttpResponse.created(commentService.addComment(chapterId, accountId, content));
    }

    @Post("/comments/{commentId}/replies")
    public HttpResponse<ReplyDTO> addReply(
            @PathVariable int commentId,
            Authentication authentication,
            @Body("content") @NotEmpty String content
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        return HttpResponse.created(commentService.addReply(commentId, accountId, content));
    }

    @Post("/comments/{commentId}/likes")
    public HttpResponse LikeOrDislikeComment(
            @PathVariable int commentId,
            Authentication authentication,
            @Body("isLike") boolean isLike
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        boolean added = commentService.likeOrDislikeComment(commentId, accountId, isLike);
        return added ? HttpResponse.ok("thành công") : HttpResponse.notFound();
    }

    @Delete("/comments/{commentId}/likes")
    public HttpResponse deleteLikeorDislike(
            @PathVariable int commentId,
            Authentication authentication
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        boolean deleted = commentService.deleteLikeOrDislike(commentId, accountId);
        return deleted ? HttpResponse.noContent() : HttpResponse.notFound();
    }

    @Delete("/comments/{commentId}")
    public HttpResponse deleteComment(
            @PathVariable int commentId,
            Authentication authentication
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        boolean deleted = commentService.deleteComment(commentId, accountId);
        return deleted ? HttpResponse.noContent() : HttpResponse.notFound();
    }
}
