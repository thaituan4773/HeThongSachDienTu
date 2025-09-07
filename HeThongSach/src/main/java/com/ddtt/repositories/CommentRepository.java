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
import com.ddtt.jooq.generated.tables.Comment;
import jakarta.transaction.Transactional;
import java.util.List;
import org.jooq.Condition;
import org.jooq.SortField;
import org.jooq.UpdateQuery;
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
                .and(COMMENT.PARENT_COMMENT_ID.isNull());

        SortField<?>[] orderFields;
        orderFields = switch (sort == null ? "" : sort.toLowerCase()) {
            case "newest" ->
                new SortField<?>[]{
                    COMMENT.CREATED_AT.desc(),
                    COMMENT.COMMENT_ID.desc()
                };
            default ->
                new SortField<?>[]{
                    COMMENT.SCORE.desc(),
                    COMMENT.CREATED_AT.desc(),
                    COMMENT.COMMENT_ID.desc()
                };
        };
        Comment child = COMMENT.as("child");
        List<CommentDTO> items = dsl.select(
                COMMENT.COMMENT_ID.as("commentId"),
                COMMENT.ACCOUNT_ID.as("accountId"),
                ACCOUNT.DISPLAY_NAME.as("displayName"),
                ACCOUNT.AVATAR_URL.as("avatarUrl"),
                COMMENT.CONTENT.as("content"),
                COMMENT.SCORE.as("score"),
                COMMENT.CREATED_AT.as("createdAt"),
                DSL.selectCount()
                        .from(child)
                        .where(child.PARENT_COMMENT_ID.eq(COMMENT.COMMENT_ID))
                        .asField("replyCount"),
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
        Condition condition = COMMENT.PARENT_COMMENT_ID.eq(parentCommentId);
        List<ReplyDTO> items = dsl.select(
                COMMENT.COMMENT_ID.as("commentId"),
                COMMENT.ACCOUNT_ID.as("accountId"),
                ACCOUNT.DISPLAY_NAME.as("displayName"),
                ACCOUNT.AVATAR_URL.as("avatarUrl"),
                COMMENT.CONTENT.as("content"),
                COMMENT.SCORE.as("score"),
                COMMENT.CREATED_AT.as("createdAt"),
                COMMENT_LIKE.IS_LIKE.as("likedByCurrentUser"),
                COMMENT.PARENT_COMMENT_ID.as("parentCommentId")
        )
                .from(COMMENT)
                .leftJoin(ACCOUNT).on(COMMENT.ACCOUNT_ID.eq(ACCOUNT.ACCOUNT_ID))
                .leftJoin(COMMENT_LIKE).on(COMMENT.COMMENT_ID.eq(COMMENT_LIKE.COMMENT_ID).and(COMMENT_LIKE.ACCOUNT_ID.eq(currentAccountId)))
                .where(condition)
                .orderBy(COMMENT.CREATED_AT.asc())
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

    public CommentDTO addComment(int chapterId, int accountId, String content) {
        // Insert comment
        var record = dsl.insertInto(COMMENT)
                .set(COMMENT.CHAPTER_ID, chapterId)
                .set(COMMENT.ACCOUNT_ID, accountId)
                .set(COMMENT.CONTENT, content)
                .set(COMMENT.SCORE, 0)
                .set(COMMENT.CREATED_AT, DSL.currentOffsetDateTime())
                .returning(COMMENT.COMMENT_ID, COMMENT.ACCOUNT_ID, COMMENT.CONTENT,
                        COMMENT.SCORE, COMMENT.CREATED_AT)
                .fetchOne();

        if (record == null) {
            throw new RuntimeException("Failed to insert comment");
        }

        var acc = dsl.select(ACCOUNT.DISPLAY_NAME, ACCOUNT.AVATAR_URL)
                .from(ACCOUNT)
                .where(ACCOUNT.ACCOUNT_ID.eq(accountId))
                .fetchOne();

        String displayName = acc != null ? acc.get(ACCOUNT.DISPLAY_NAME) : null;
        String avatarUrl = acc != null ? acc.get(ACCOUNT.AVATAR_URL) : null;

        return new CommentDTO(
                record.getCommentId(),
                record.getAccountId(),
                displayName,
                avatarUrl,
                record.getContent(),
                record.getScore(),
                record.getCreatedAt(),
                0, // replyCount
                null // likedByCurrentUser
        );
    }

    public ReplyDTO addReply(int parentCommentId, int accountId, String content) {
        // Kiểm tra parent comment có tồn tại và chưa bị xóa
        var parentExists = dsl.fetchExists(
                dsl.selectOne()
                        .from(COMMENT)
                        .where(COMMENT.COMMENT_ID.eq(parentCommentId))
        );

        if (!parentExists) {
            throw new RuntimeException("Parent comment not found or deleted");
        }

        // Lấy chapter_id từ parent comment để gán vào reply
        Integer chapterId = dsl.select(COMMENT.CHAPTER_ID)
                .from(COMMENT)
                .where(COMMENT.COMMENT_ID.eq(parentCommentId))
                .fetchOneInto(Integer.class);

        // Insert reply
        var record = dsl.insertInto(COMMENT)
                .set(COMMENT.CHAPTER_ID, chapterId)
                .set(COMMENT.ACCOUNT_ID, accountId)
                .set(COMMENT.CONTENT, content)
                .set(COMMENT.PARENT_COMMENT_ID, parentCommentId)
                .set(COMMENT.SCORE, 0)
                .set(COMMENT.CREATED_AT, DSL.currentOffsetDateTime())
                .returning(COMMENT.COMMENT_ID, COMMENT.ACCOUNT_ID, COMMENT.CONTENT,
                        COMMENT.SCORE, COMMENT.CREATED_AT, COMMENT.PARENT_COMMENT_ID)
                .fetchOne();

        if (record == null) {
            throw new RuntimeException("Failed to insert reply");
        }

        // Lấy thông tin account
        var acc = dsl.select(ACCOUNT.DISPLAY_NAME, ACCOUNT.AVATAR_URL)
                .from(ACCOUNT)
                .where(ACCOUNT.ACCOUNT_ID.eq(accountId))
                .fetchOne();

        String displayName = acc != null ? acc.get(ACCOUNT.DISPLAY_NAME) : null;
        String avatarUrl = acc != null ? acc.get(ACCOUNT.AVATAR_URL) : null;

        return new ReplyDTO(
                record.getCommentId(),
                record.getAccountId(),
                displayName,
                avatarUrl,
                record.getContent(),
                record.getScore(),
                record.getCreatedAt(),
                null, // likedByCurrentUser
                record.getParentCommentId()
        );
    }

    public boolean likeOrDislikeComment(int commentId, int accountId, boolean isLike) {
        dsl.insertInto(COMMENT_LIKE)
                .set(COMMENT_LIKE.COMMENT_ID, commentId)
                .set(COMMENT_LIKE.ACCOUNT_ID, accountId)
                .set(COMMENT_LIKE.IS_LIKE, isLike)
                .onConflict(COMMENT_LIKE.COMMENT_ID, COMMENT_LIKE.ACCOUNT_ID)
                .doUpdate()
                .set(COMMENT_LIKE.IS_LIKE, isLike)
                .execute();

        return true;
    }

    public String updateCommentContent(int accountId, int commentId, String newContent) {
        int updated = dsl.update(COMMENT)
                .set(COMMENT.CONTENT, newContent)
                .where(COMMENT.COMMENT_ID.eq(commentId))
                .and(COMMENT.ACCOUNT_ID.eq(accountId))
                .execute();

        if (updated == 0) {
            throw new IllegalArgumentException("Comment không tồn tại hoặc không thuộc về tài khoản");
        }

        return newContent;
    }

    public boolean deleteLikeOrDislike(int commentId, int accountId) {
        int result = dsl.deleteFrom(COMMENT_LIKE)
                .where(COMMENT_LIKE.COMMENT_ID.eq(commentId)
                        .and(COMMENT_LIKE.ACCOUNT_ID.eq(accountId)))
                .execute();

        return result > 0;
    }

    public boolean deleteComment(int accountId, int commentId) {
        int deleted = dsl.deleteFrom(COMMENT)
                .where(COMMENT.COMMENT_ID.eq(commentId))
                .and(COMMENT.ACCOUNT_ID.eq(accountId))
                .execute();

        if (deleted == 0) {
            throw new IllegalArgumentException("Comment không tồn tại hoặc không thuộc về tài khoản");
        }

        return true;
    }
}
