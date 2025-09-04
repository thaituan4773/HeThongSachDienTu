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

    public boolean likeOrDislikeComment(int commentId, int accountId, boolean isLike) {
        return commentRepository.likeOrDislikeComment(commentId, accountId, isLike);
    }

    public boolean deleteLikeOrDislike(int commentId, int accountId) {
        return commentRepository.deleteLikeOrDislike(commentId, accountId);
    }
    
    public CommentDTO addComment(int chapterId, int accountId, String content) {
        return commentRepository.addComment(chapterId, accountId, content);
    }
    
    public ReplyDTO addReply(int parentCommentId, int accountId, String content){
        return commentRepository.addReply(parentCommentId, accountId, content);
    }
    
    public boolean deleteComment(int commentId, int accountId) {
        return commentRepository.deleteComment(commentId, accountId);
    }
}
