package com.ddtt.repositories;

import com.ddtt.dtos.ChapterContentDTO;
import com.ddtt.dtos.ChapterCreateDTO;
import com.ddtt.dtos.ChapterOverviewDTO;
import static com.ddtt.jooq.generated.tables.BookView.BOOK_VIEW;
import static com.ddtt.jooq.generated.tables.Chapter.CHAPTER;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import static com.ddtt.jooq.generated.tables.ChapterPurchase.CHAPTER_PURCHASE;
import static com.ddtt.jooq.generated.tables.Comment.COMMENT;
import static com.ddtt.jooq.generated.tables.ReadingProgress.READING_PROGRESS;
import com.ddtt.repository.conditions.ChapterConditions;
import io.micronaut.core.annotation.Blocking;
import java.time.OffsetDateTime;
import java.util.List;
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

    private Field<Boolean> hasUnlockedField(int accountId) {
        return DSL.case_()
                .when(CHAPTER.COIN_PRICE.eq(0), DSL.inline(true))
                .when(
                        DSL.exists(
                                DSL.selectOne()
                                        .from(CHAPTER_PURCHASE)
                                        .where(CHAPTER_PURCHASE.CHAPTER_ID.eq(CHAPTER.CHAPTER_ID))
                                        .and(CHAPTER_PURCHASE.ACCOUNT_ID.eq(accountId))
                        ),
                        DSL.inline(true)
                )
                .otherwise(DSL.inline(false));
    }

    public boolean hasChapterAccess(int chapterId, int accountId) {
        return Boolean.TRUE.equals(
                dsl.select(hasUnlockedField(accountId))
                        .from(CHAPTER)
                        .where(CHAPTER.CHAPTER_ID.eq(chapterId))
                        .fetchOneInto(Boolean.class)
        );
    }

    public ChapterContentDTO readChapterContent(int chapterId, int accountId) {
        var currentChapter = CHAPTER.as("current_chapter");
        var otherChapter = CHAPTER.as("other_chapter");

        var dto = dsl.select(
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
        recordView(chapterId, accountId);
        recordProgress(chapterId, accountId);
        return dto;
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

    private void recordView(int chapterId, int accountId) {
        int bookId = dsl.select(CHAPTER.BOOK_ID)
                .from(CHAPTER)
                .where(CHAPTER.CHAPTER_ID.eq(chapterId))
                .fetchOne(CHAPTER.BOOK_ID);
        dsl.insertInto(BOOK_VIEW)
                .set(BOOK_VIEW.BOOK_ID, bookId)
                .set(BOOK_VIEW.ACCOUNT_ID, accountId)
                .set(BOOK_VIEW.VIEWED_AT, OffsetDateTime.now())
                .onConflict(BOOK_VIEW.ACCOUNT_ID, BOOK_VIEW.BOOK_ID)
                .doUpdate()
                .set(BOOK_VIEW.VIEWED_AT, OffsetDateTime.now())
                .execute();
    }

    private void recordProgress(int chapterId, int accountId) {
        // Lấy bookId và position
        var chapter = dsl.select(CHAPTER.BOOK_ID, CHAPTER.POSITION)
                .from(CHAPTER)
                .where(CHAPTER.CHAPTER_ID.eq(chapterId))
                .fetchOne();

        if (chapter == null) {
            throw new IllegalArgumentException("Chapter not found: " + chapterId);
        }

        int bookId = chapter.get(CHAPTER.BOOK_ID);
        int chapterPos = chapter.get(CHAPTER.POSITION);
        int bitIndex = chapterPos - 1;                 // -1 vì index bắt đầu từ 0
        int bytesNeeded = (bitIndex / 8) + 1;          // tính số byte cần lưu

        dsl.insertInto(READING_PROGRESS)
                .set(READING_PROGRESS.ACCOUNT_ID, accountId)
                .set(READING_PROGRESS.BOOK_ID, bookId)
                .set(READING_PROGRESS.READ_CHAPTERS_BITMAP, // Thêm bitmap mới
                        DSL.field(
                                "set_bit(decode(repeat('00', {0}), 'hex'), {1}, 1)",
                                byte[].class,
                                bytesNeeded,
                                bitIndex
                        ))
                .onConflict(READING_PROGRESS.ACCOUNT_ID, READING_PROGRESS.BOOK_ID)
                .doUpdate()
                .set(READING_PROGRESS.READ_CHAPTERS_BITMAP, // Cập nhật bitmap hiện tại
                        DSL.field(
                                "set_bit(COALESCE(reading_progress.read_chapters_bitmap, ''::bytea) || "
                                + "decode(repeat('00', {0} - octet_length(reading_progress.read_chapters_bitmap)), 'hex'), {1}, 1)",
                                byte[].class,
                                bytesNeeded,
                                bitIndex
                        ))
                .set(READING_PROGRESS.LAST_UPDATED_AT, DSL.currentOffsetDateTime())
                .execute();
    }

    public List<ChapterOverviewDTO> getChaptersInfo(int bookId, int accountId) {
        // chỉ gọi get_bit khi bitmap có đủ bit
        Field<Boolean> hasReadField = DSL.field(
                "CASE WHEN COALESCE(octet_length(reading_progress.read_chapters_bitmap), 0) * 8 > ({0} - 1) "
                + "THEN get_bit(reading_progress.read_chapters_bitmap, {0} - 1) = 1 ELSE false END",
                Boolean.class,
                CHAPTER.POSITION
        );

        return dsl.select(
                CHAPTER.CHAPTER_ID.as("chapterId"),
                CHAPTER.TITLE.as("title"),
                CHAPTER.POSITION.as("order"),
                CHAPTER.COIN_PRICE.as("coinPrice"),
                CHAPTER.CREATED_AT.as("createdDate"),
                hasReadField.as("hasRead"),
                hasUnlockedField(accountId).as("hasUnlocked")
        )
                .from(CHAPTER)
                .leftJoin(READING_PROGRESS)
                .on(READING_PROGRESS.ACCOUNT_ID.eq(accountId)
                        .and(READING_PROGRESS.BOOK_ID.eq(CHAPTER.BOOK_ID)))
                .where(CHAPTER.BOOK_ID.eq(bookId))
                .and(ChapterConditions.isPublished())
                .orderBy(CHAPTER.POSITION.asc())
                .fetchInto(ChapterOverviewDTO.class);
    }

}
