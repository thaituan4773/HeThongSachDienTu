package com.ddtt.repositories;

import com.ddtt.dtos.ChapterAccessDTO;
import com.ddtt.dtos.ChapterContentDTO;
import com.ddtt.dtos.ChapterCreateDTO;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import static com.ddtt.jooq.generated.tables.ChapterPurchase.CHAPTER_PURCHASE;
import static com.ddtt.jooq.generated.tables.Chapter.CHAPTER;
import static com.ddtt.jooq.generated.tables.Comment.COMMENT;
import com.ddtt.repository.conditions.ChapterConditions;
import io.micronaut.core.annotation.Blocking;
import java.time.OffsetDateTime;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

@Singleton
@Blocking
@RequiredArgsConstructor
public class ChapterRepository {

    private final DSLContext dsl;

    public Condition readableChapter() {
        return CHAPTER.STATUS.eq("published")
                .and(CHAPTER.DELETED_AT.isNull());
    }

    public ChapterAccessDTO getChapterAccess(int chapterId, Integer userId) {
        Field<Boolean> alreadyPurchased;

        if (userId != null) {
            alreadyPurchased = DSL.exists(
                    dsl.selectOne()
                            .from(CHAPTER_PURCHASE)
                            .where(CHAPTER_PURCHASE.CHAPTER_ID.eq(chapterId))
                            .and(CHAPTER_PURCHASE.ACCOUNT_ID.eq(userId))
            ).cast(Boolean.class);
        } else {
            alreadyPurchased = DSL.val(false);
        }

        return dsl.select(
                CHAPTER.CHAPTER_ID.as("chapterId"),
                CHAPTER.COIN_PRICE.as("coinPrice"),
                alreadyPurchased.as("alreadyPurchased")
        )
                .from(CHAPTER)
                .where(CHAPTER.CHAPTER_ID.eq(chapterId))
                .fetchOneInto(ChapterAccessDTO.class);
    }

    public ChapterContentDTO getChapterContent(int chapterId) {
        var currentChapter = CHAPTER.as("current_chapter");
        var otherChapter = CHAPTER.as("other_chapter");

        return dsl.select(
                currentChapter.CHAPTER_ID.as("chapterId"),
                currentChapter.TITLE.as("title"),
                currentChapter.CONTENT.as("content"),
                // chương trước
                DSL.select(DSL.max(otherChapter.CHAPTER_ID))
                        .from(otherChapter)
                        .where(otherChapter.BOOK_ID.eq(currentChapter.BOOK_ID))
                        .and(otherChapter.POSITION.lt(currentChapter.POSITION))
                        .and(ChapterConditions.isPublished(otherChapter))
                        .asField("prevChapterId"),
                // chương sau
                DSL.select(DSL.min(otherChapter.CHAPTER_ID))
                        .from(otherChapter)
                        .where(otherChapter.BOOK_ID.eq(currentChapter.BOOK_ID))
                        .and(otherChapter.POSITION.gt(currentChapter.POSITION))
                        .and(ChapterConditions.isPublished(otherChapter))
                        .asField("nextChapterId"),
                // đếm comment
                DSL.selectCount()
                        .from(COMMENT)
                        .where(COMMENT.CHAPTER_ID.eq(currentChapter.CHAPTER_ID))
                        .asField("commentCount")
        )
                .from(currentChapter)
                .where(currentChapter.CHAPTER_ID.eq(chapterId))
                .fetchOneInto(ChapterContentDTO.class);
    }

    public int addChapter(ChapterCreateDTO dto) {
        String status = dto.getStatus() != null ? dto.getStatus() : "PUBLISHED";
        OffsetDateTime createdAt = OffsetDateTime.now();

        return dsl.insertInto(CHAPTER)
                .columns(
                        CHAPTER.BOOK_ID,
                        CHAPTER.POSITION,
                        CHAPTER.TITLE,
                        CHAPTER.CONTENT,
                        CHAPTER.COIN_PRICE,
                        CHAPTER.STATUS,
                        CHAPTER.CREATED_AT
                )
                .values(dto.getBookId(), dto.getPosition(), dto.getTitle(), dto.getContent(), dto.getCoinPrice(), status, createdAt)
                .returning(CHAPTER.CHAPTER_ID)
                .fetchOne()
                .getChapterId();
    }

}
