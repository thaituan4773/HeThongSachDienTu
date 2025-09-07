package com.ddtt.repositories;

import com.ddtt.dtos.ChapterContentDTO;
import com.ddtt.dtos.ChapterInputDTO;
import com.ddtt.dtos.ChapterOverviewDTO;
import com.ddtt.dtos.ChapterUpdateDTO;
import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.exceptions.DuplicateException;
import com.ddtt.exceptions.NotFoundException;
import com.ddtt.exceptions.PaymentRequiredException;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import static com.ddtt.jooq.generated.tables.ChapterPurchase.CHAPTER_PURCHASE;
import static com.ddtt.jooq.generated.tables.Comment.COMMENT;
import static com.ddtt.jooq.generated.tables.ReadingProgress.READING_PROGRESS;
import static com.ddtt.jooq.generated.tables.BookView.BOOK_VIEW;
import static com.ddtt.jooq.generated.tables.Chapter.CHAPTER;
import static com.ddtt.jooq.generated.tables.Account.ACCOUNT;
import static com.ddtt.jooq.generated.tables.Book.BOOK;
import com.ddtt.repository.conditions.ChapterConditions;
import io.micronaut.core.annotation.Blocking;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.UpdateQuery;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

@Singleton
@Blocking
@RequiredArgsConstructor
public class ChapterRepository {

    private final DSLContext dsl;

    private Field<Boolean> hasUnlockedField(int accountId) {
        return DSL.case_()
                .when(CHAPTER.COIN_PRICE.eq(0), DSL.inline(true))
                .when(
                        DSL.exists(
                                DSL.selectOne()
                                        .from(BOOK)
                                        .where(BOOK.BOOK_ID.eq(CHAPTER.BOOK_ID))
                                        .and(BOOK.AUTHOR_ACCOUNT_ID.eq(accountId))
                        ),
                        DSL.inline(true)
                )
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

    @Transactional
    public ChapterInputDTO addChapter(int accountId, int bookId, ChapterInputDTO dto) {
        boolean bookExists = dsl.fetchExists(
                DSL.selectOne()
                        .from(BOOK)
                        .where(BOOK.BOOK_ID.eq(bookId))
                        .and(BOOK.AUTHOR_ACCOUNT_ID.eq(accountId))
                        .and(BOOK.DELETED_AT.isNull())
        );

        if (!bookExists) {
            throw new IllegalArgumentException("Sách không tồn tại hoặc không thuộc về tác giả");
        }

        OffsetDateTime createdAt = OffsetDateTime.now();
        int newPosition = dto.getPosition();

        // di chuyển position chương trước nó
        dsl.update(CHAPTER)
                .set(CHAPTER.POSITION, CHAPTER.POSITION.plus(1))
                .where(CHAPTER.BOOK_ID.eq(bookId))
                .and(CHAPTER.POSITION.ge(newPosition))
                .execute();

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
                .values(bookId, newPosition, dto.getTitle(), dto.getContent(), dto.getCoinPrice(), dto.getStatus(), createdAt)
                .returning(
                        CHAPTER.CHAPTER_ID.as("chapterId"),
                        CHAPTER.POSITION.as("order"),
                        CHAPTER.TITLE.as("title"),
                        CHAPTER.CONTENT.as("content"),
                        CHAPTER.COIN_PRICE.as("coinPrice"),
                        CHAPTER.STATUS.as("status"),
                        CHAPTER.CREATED_AT.as("createdAt")
                )
                .fetchOptionalInto(ChapterInputDTO.class)
                .orElseThrow(() -> new IllegalStateException("Không thể tạo chapter mới"));

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

    public PageResponseDTO<ChapterOverviewDTO> getChaptersInfoPaged(
            int bookId,
            int accountId,
            int page,
            int size,
            boolean desc // true = chapter cuối, false = chapter đầu
    ) {
        int offset = (page - 1) * size;

        // chỉ gọi get_bit khi bitmap có đủ bit
        Field<Boolean> hasReadField = DSL.field(
                "CASE WHEN COALESCE(octet_length(reading_progress.read_chapters_bitmap), 0) * 8 > ({0} - 1) "
                + "THEN get_bit(reading_progress.read_chapters_bitmap, {0} - 1) = 1 ELSE false END",
                Boolean.class,
                CHAPTER.POSITION
        );

        Field<Boolean> hasUnlockedF = hasUnlockedField(accountId);

        Condition condition = CHAPTER.BOOK_ID.eq(bookId).and(ChapterConditions.isPublished());

        List<ChapterOverviewDTO> items = dsl.select(
                CHAPTER.CHAPTER_ID.as("chapterId"),
                CHAPTER.TITLE.as("title"),
                CHAPTER.POSITION.as("order"),
                CHAPTER.COIN_PRICE.as("coinPrice"),
                CHAPTER.CREATED_AT.as("createdDate"),
                hasReadField.as("hasRead"),
                hasUnlockedF.as("hasUnlocked")
        )
                .from(CHAPTER)
                .leftJoin(READING_PROGRESS)
                .on(READING_PROGRESS.ACCOUNT_ID.eq(accountId)
                        .and(READING_PROGRESS.BOOK_ID.eq(CHAPTER.BOOK_ID)))
                .where(condition)
                .orderBy(desc ? CHAPTER.POSITION.desc() : CHAPTER.POSITION.asc())
                .limit(size)
                .offset(offset)
                .fetchInto(ChapterOverviewDTO.class);

        // Tính tổng số chương và tổng số trang (chỉ tính trang đầu)
        Long total = null;
        Integer totalPages = null;
        if (page == 1) {
            long cnt = dsl.fetchCount(
                    dsl.select(CHAPTER.CHAPTER_ID)
                            .from(CHAPTER)
                            .where(condition)
            );
            total = cnt;
            totalPages = (int) Math.ceil((double) cnt / size);
        }

        return new PageResponseDTO<>(total, page, size, totalPages, items);
    }

    @Transactional
    public void unlockChapter(int accountId, int chapterId) {
        // Lấy giá chapter
        Integer price = dsl.select(CHAPTER.COIN_PRICE)
                .from(CHAPTER)
                .where(CHAPTER.CHAPTER_ID.eq(chapterId))
                .fetchOneInto(Integer.class);

        if (price == null) {
            throw new NotFoundException("Không tìm thấy chương");
        }

        // Trừ tiền
        int updated = dsl.update(ACCOUNT)
                .set(ACCOUNT.BALANCE, ACCOUNT.BALANCE.minus(price))
                .where(ACCOUNT.ACCOUNT_ID.eq(accountId))
                .and(ACCOUNT.BALANCE.ge(price))
                .execute();

        if (updated == 0) {
            throw new PaymentRequiredException("Số dư không đủ");
        }

        try {
            // Ghi purchase
            dsl.insertInto(CHAPTER_PURCHASE)
                    .set(CHAPTER_PURCHASE.ACCOUNT_ID, accountId)
                    .set(CHAPTER_PURCHASE.CHAPTER_ID, chapterId)
                    .set(CHAPTER_PURCHASE.COIN_PRICE, price)
                    .execute();
        } catch (DataAccessException e) {
            if (DuplicateException.isDuplicate(e)) {
                throw new DuplicateException("Chương đã được mở khóa");
            }
            throw e;
        }
    }

    public ChapterUpdateDTO updateChapter(int accountId, int chapterId, ChapterUpdateDTO dto) {

        boolean bookExists = dsl.fetchExists(
                DSL.selectOne()
                        .from(CHAPTER)
                        .join(BOOK).on(CHAPTER.BOOK_ID.eq(BOOK.BOOK_ID))
                        .where(CHAPTER.CHAPTER_ID.eq(chapterId))
                        .and(BOOK.AUTHOR_ACCOUNT_ID.eq(accountId))
                        .and(BOOK.DELETED_AT.isNull())
        );

        if (!bookExists) {
            throw new IllegalArgumentException("Sách không tồn tại hoặc không thuộc về tác giả");
        }

        UpdateQuery<?> uq = dsl.updateQuery(CHAPTER);

        if (dto.getTitle() != null) {
            uq.addValue(CHAPTER.TITLE, dto.getTitle());
        }
        if (dto.getContent() != null) {
            uq.addValue(CHAPTER.CONTENT, dto.getContent());
        }
        if (dto.getPosition() != null) {
            uq.addValue(CHAPTER.POSITION, dto.getPosition());
        }
        if (dto.getCoinPrice() != null) {
            uq.addValue(CHAPTER.COIN_PRICE, dto.getCoinPrice());
        }
        if (dto.getStatus() != null) {
            uq.addValue(CHAPTER.STATUS, dto.getStatus());
        }

        uq.addConditions(CHAPTER.CHAPTER_ID.eq(chapterId));

        int updated = uq.execute();
        if (updated == 0) {
            throw new NotFoundException("Chapter không tồn tại hoặc không có trường nào được cập nhật");
        }

        return dsl.select(
                CHAPTER.TITLE.as("title"),
                CHAPTER.POSITION.as("order"),
                CHAPTER.COIN_PRICE.as("coinPrice"),
                CHAPTER.STATUS.as("status"),
                CHAPTER.CONTENT.as("content")
        )
                .from(CHAPTER)
                .where(CHAPTER.CHAPTER_ID.eq(chapterId))
                .fetchOneInto(ChapterUpdateDTO.class);
    }

    @Transactional
    public void softDeleteChapter(int chapterId) {
        OffsetDateTime now = OffsetDateTime.now();

        int updated = dsl.update(CHAPTER)
                .set(CHAPTER.DELETED_AT, now)
                .where(CHAPTER.CHAPTER_ID.eq(chapterId))
                .execute();
        if (updated == 0) {
            throw new IllegalArgumentException("Chapter không tồn tại");
        }
        dsl.deleteFrom(COMMENT)
                .where(COMMENT.CHAPTER_ID.eq(chapterId))
                .execute();
    }

}
