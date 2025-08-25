package com.ddtt.repositories;

import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.dtos.PersonalLibraryBookDTO;
import io.micronaut.core.annotation.Blocking;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import static com.ddtt.jooq.generated.tables.Book.BOOK;
import static com.ddtt.jooq.generated.tables.Chapter.CHAPTER;
import static com.ddtt.jooq.generated.tables.PersonalLibrary.PERSONAL_LIBRARY;
import static com.ddtt.jooq.generated.tables.ReadingProgress.READING_PROGRESS;
import com.ddtt.repository.conditions.ChapterConditions;
import java.time.OffsetDateTime;
import org.jooq.Field;
import org.jooq.SortField;
import org.jooq.impl.DSL;

@Singleton
@Blocking
@RequiredArgsConstructor
public class PersonalLibraryRepository {

    private final DSLContext dsl;

    public PageResponseDTO<PersonalLibraryBookDTO> getPersonalLibrary(
            int accountId,
            int page,
            int size,
            String sortBy,
            boolean desc,
            Boolean unreaded // filter null (lấy hết), có (lấy unreaded), không (lấy đã đoc hết)
    ) {
        int offset = (page - 1) * size;

        // Subquery đếm tổng chương của mỗi book
        var chapterCount = dsl.select(
                CHAPTER.BOOK_ID,
                DSL.count().as("totalChapters")
        )
                .from(CHAPTER)
                .where(ChapterConditions.isPublished())
                .groupBy(CHAPTER.BOOK_ID)
                .asTable("chapterCount");

        // field số chương đã đọc, tính từ bit_count()
        Field<Integer> readChaptersField = DSL.field(
                "bit_count({0})", Integer.class,
                READING_PROGRESS.READ_CHAPTERS_BITMAP
        ).as("readChapters");

        Field<Integer> totalChaptersField = DSL.coalesce(chapterCount.field("totalChapters", Integer.class), DSL.inline(0));

        Field<Integer> unreadChaptersField = totalChaptersField
                .minus(DSL.coalesce(readChaptersField, DSL.inline(0)))
                .as("unreadChapters");

        // Câu query chính
        var base = dsl.select(
                BOOK.BOOK_ID.as("bookId"),
                BOOK.TITLE.as("title"),
                BOOK.COVER_IMAGE_URL.as("coverImageURL"),
                totalChaptersField.as("totalChapters"),
                unreadChaptersField,
                PERSONAL_LIBRARY.FOLLOWED_AT.as("addedAt"),
                READING_PROGRESS.LAST_UPDATED_AT.as("lastReadAt")
        )
                .from(PERSONAL_LIBRARY)
                .join(BOOK).on(PERSONAL_LIBRARY.BOOK_ID.eq(BOOK.BOOK_ID))
                .leftJoin(chapterCount).on(chapterCount.field(CHAPTER.BOOK_ID).eq(BOOK.BOOK_ID))
                .leftJoin(READING_PROGRESS).on(READING_PROGRESS.ACCOUNT_ID.eq(PERSONAL_LIBRARY.ACCOUNT_ID)
                .and(READING_PROGRESS.BOOK_ID.eq(BOOK.BOOK_ID)))
                .where(PERSONAL_LIBRARY.ACCOUNT_ID.eq(accountId));

        // filter unreaded
        if (Boolean.TRUE.equals(unreaded)) {
            base = base.and(unreadChaptersField.gt(0));
        } else if (Boolean.FALSE.equals(unreaded)) {
            base = base.and(unreadChaptersField.eq(0));
        }

        // sort
        SortField<?> sortField;
        sortField = switch (sortBy != null ? sortBy : "title") {
            case "unreadChapters" ->
                desc ? unreadChaptersField.desc() : unreadChaptersField.asc();
            case "totalChapters" ->
                desc ? totalChaptersField.desc() : totalChaptersField.asc();
            case "lastReadAt" ->
                desc ? READING_PROGRESS.LAST_UPDATED_AT.desc() : READING_PROGRESS.LAST_UPDATED_AT.asc();
            case "addedAt" ->
                desc ? PERSONAL_LIBRARY.FOLLOWED_AT.desc() : PERSONAL_LIBRARY.FOLLOWED_AT.asc();
            default ->
                desc ? BOOK.TITLE.desc() : BOOK.TITLE.asc();
        }; // "title"

        // query dữ liệu trang đầu tiên
        var items = base
                .orderBy(sortField)
                .limit(size)
                .offset(offset)
                .fetch(record -> new PersonalLibraryBookDTO(
                record.get("bookId", Integer.class),
                record.get("title", String.class),
                record.get("coverImageURL", String.class),
                record.get("totalChapters", Integer.class),
                record.get("unreadChapters", Integer.class),
                record.get("addedAt", OffsetDateTime.class),
                record.get("lastReadAt", OffsetDateTime.class)
        ));

        // tổng (chỉ tính ở page 1)
        Long total = null;
        Integer totalPages = null;
        if (page == 1) {
            total = (long) dsl.fetchCount(base);
            totalPages = (int) Math.ceil((double) total / size);
        }

        return new PageResponseDTO<>(
                total,
                page,
                size,
                totalPages,
                items
        );
    }
}
