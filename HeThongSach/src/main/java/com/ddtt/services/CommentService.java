package com.ddtt.services;

import com.ddtt.dtos.CommentDTO;
import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.dtos.ReplyDTO;
import com.ddtt.repositories.CommentRepository;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final int pageSize = 30;

    public PageResponseDTO<CommentDTO> getCommentsByChapter(
            int chapterId,
            int page,
            int currentAccountId,
            String sort
    ) {
        return commentRepository.getCommentsByChapter(chapterId, page, pageSize, currentAccountId, sort);
    }

    public PageResponseDTO<ReplyDTO> getRepliesByComment(
            int commentId,
            int page,
            int currentAccountId
    ) {
        return commentRepository.getRepliesByComment(commentId, page, pageSize, currentAccountId);
    }
}
