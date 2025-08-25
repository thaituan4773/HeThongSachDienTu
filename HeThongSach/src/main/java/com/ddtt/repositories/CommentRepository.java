package com.ddtt.repositories;

import com.ddtt.dtos.CommentDTO;
import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.dtos.ReplyDTO;
import io.micronaut.core.annotation.Blocking;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import static com.ddtt.jooq.generated.tables.Comment.COMMENT;
import static com.ddtt.jooq.generated.tables.Account.ACCOUNT;
import static com.ddtt.jooq.generated.tables.CommentLike.COMMENT_LIKE;
import java.util.List;
import org.jooq.Condition;
import org.jooq.SortField;
import org.jooq.impl.DSL;

@Singleton
@Blocking
@RequiredArgsConstructor
public class CommentRepository {

    private final DSLContext dsl;

    public PageResponseDTO<CommentDTO> getCommentsByChapter(
            int chapterId,
            int page,
            int size,
            int currentAccountId,
            String sort
    ) {
        int offset = (page - 1) * size;

        Condition condition = COMMENT.CHAPTER_ID.eq(chapterId)
                .and(COMMENT.PARENT_COMMENT_ID.isNull())
                .and(COMMENT.DELETED_AT.isNull());

        SortField<?>[] orderFields;
        orderFields = switch (sort == null ? "" : sort.toLowerCase()) {
            case "mostliked" ->
                new SortField<?>[]{
                    COMMENT.CREATED_AT.desc(),
                    COMMENT.COMMENT_ID.desc()
                };
            default ->
                new SortField<?>[]{
                    COMMENT.LIKES_COUNT.desc(),
                    COMMENT.CREATED_AT.desc(),
                    COMMENT.COMMENT_ID.desc()
                };
        };

        List<CommentDTO> items = dsl.select(
                COMMENT.COMMENT_ID.as("commentId"),
                COMMENT.ACCOUNT_ID.as("accountId"),
                ACCOUNT.DISPLAY_NAME.as("displayName"),
                ACCOUNT.AVATAR_URL.as("avatarUrl"),
                COMMENT.CONTENT.as("content"),
                COMMENT.LIKES_COUNT.as("likesCount"),
                COMMENT.CREATED_AT.as("createdAt"),
                DSL.coalesce(
                        DSL.selectCount()
                                .from(COMMENT)
                                .where(COMMENT.PARENT_COMMENT_ID.eq(COMMENT.COMMENT_ID)
                                        .and(COMMENT.DELETED_AT.isNull())), DSL.inline(0))
                        .as("replyCount"),
                COMMENT_LIKE.IS_LIKE.as("likedByCurrentUser")
        )
                .from(COMMENT)
                .leftJoin(ACCOUNT).on(COMMENT.ACCOUNT_ID.eq(ACCOUNT.ACCOUNT_ID))
                .leftJoin(COMMENT_LIKE).on(COMMENT.COMMENT_ID.eq(COMMENT_LIKE.COMMENT_ID).and(COMMENT_LIKE.ACCOUNT_ID.eq(currentAccountId)))
                .where(condition)
                .orderBy(orderFields)
                .limit(size)
                .offset(offset)
                .fetchInto(CommentDTO.class);

        Long total = null;
        Integer totalPages = null;
        if (page == 1) {
            long cnt = dsl.fetchCount(dsl.select(COMMENT.COMMENT_ID).from(COMMENT).where(condition));
            total = cnt;
            totalPages = (int) Math.ceil((double) cnt / size);
        }

        return new PageResponseDTO<>(total, page, size, totalPages, items);
    }

    public PageResponseDTO<ReplyDTO> getRepliesByComment(int parentCommentId, int page, int size, int currentAccountId) {
        int offset = (page - 1) * size;
        Condition condition = COMMENT.PARENT_COMMENT_ID.eq(parentCommentId)
                .and(COMMENT.DELETED_AT.isNull());
        List<ReplyDTO> items = dsl.select(
                COMMENT.COMMENT_ID.as("commentId"),
                COMMENT.ACCOUNT_ID.as("accountId"),
                ACCOUNT.DISPLAY_NAME.as("displayName"),
                ACCOUNT.AVATAR_URL.as("avatarUrl"),
                COMMENT.CONTENT.as("content"),
                COMMENT.LIKES_COUNT.as("likesCount"),
                COMMENT.CREATED_AT.as("createdAt"),
                COMMENT_LIKE.IS_LIKE.as("likedByCurrentUser"),
                COMMENT.PARENT_COMMENT_ID.as("parentCommentId")
        )
                .from(COMMENT)
                .leftJoin(ACCOUNT).on(COMMENT.ACCOUNT_ID.eq(ACCOUNT.ACCOUNT_ID))
                .leftJoin(COMMENT_LIKE).on(COMMENT.COMMENT_ID.eq(COMMENT_LIKE.COMMENT_ID).and(COMMENT_LIKE.ACCOUNT_ID.eq(currentAccountId)))
                .where(condition)
                .limit(size)
                .offset(offset)
                .fetchInto(ReplyDTO.class);
        
        Long total = null;
        Integer totalPages = null;
        if (page == 1) {
            long cnt = dsl.fetchCount(dsl.select(COMMENT.COMMENT_ID).from(COMMENT).where(condition));
            total = cnt;
            totalPages = (int) Math.ceil((double) cnt / size);
        }

        return new PageResponseDTO<>(total, page, size, totalPages, items);
    }

}
