package com.ddtt.repositories;

import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.dtos.PersonalLibraryBookDTO;
import com.ddtt.exceptions.DuplicateException;
import io.micronaut.core.annotation.Blocking;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import static com.ddtt.jooq.generated.tables.Book.BOOK;
import static com.ddtt.jooq.generated.tables.Chapter.CHAPTER;
import static com.ddtt.jooq.generated.tables.PersonalLibrary.PERSONAL_LIBRARY;
import static com.ddtt.jooq.generated.tables.ReadingProgress.READING_PROGRESS;
import com.ddtt.repository.conditions.ChapterConditions;
import jakarta.persistence.EntityExistsException;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import org.jooq.Condition;
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

        var base = dsl.select(
                BOOK.BOOK_ID.as("bookId"),
                BOOK.TITLE.as("title"),
                BOOK.COVER_IMAGE_URL.as("coverImageURL"),
                totalChaptersField.as("totalChapters"),
                unreadChaptersField.as("unreadChapters"),
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
        };

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

    @Transactional
    public void addBookToLibrary(int accountId, int bookId) {
        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(PERSONAL_LIBRARY)
                        .where(PERSONAL_LIBRARY.ACCOUNT_ID.eq(accountId))
                        .and(PERSONAL_LIBRARY.BOOK_ID.eq(bookId))
        );

        if (exists) {
            throw new DuplicateException(accountId, bookId);
        }
        // Insert mới
        dsl.insertInto(PERSONAL_LIBRARY)
                .set(PERSONAL_LIBRARY.ACCOUNT_ID, accountId)
                .set(PERSONAL_LIBRARY.BOOK_ID, bookId)
                .set(PERSONAL_LIBRARY.FOLLOWED_AT, OffsetDateTime.now())
                .execute();
    }

//    public List<PersonalLibraryBookDTO> searchLibraryBooks(int accountId, String keyword) {
//        if (keyword == null || keyword.isBlank()) {
//            return Collections.emptyList();
//        }
//
//        String kw = keyword.trim().toLowerCase();
//
//        Condition prefixMatch = DSL.lower(BOOK.TITLE).likeIgnoreCase(kw + "%");
//        Condition containsMatch = DSL.lower(BOOK.TITLE).likeIgnoreCase("%" + kw + "%");
//
//        return dsl.select(
//                BOOK.BOOK_ID,
//                BOOK.TITLE,
//                BOOK.COVER_IMAGE_URL,
//                BOOK.TOTAL_CHAPTERS,
//                BOOK.TOTAL_CHAPTERS.minus(DSL.coalesce(READ_PROGRESS.LAST_READ_CHAPTER, 0)).as("unreadChapters"),
//                PERSONAL_LIBRARY.FOLLOWED_AT.as("addedAt"),
//                READ_PROGRESS.LAST_READ_AT.as("lastReadAt")
//        )
//                .from(PERSONAL_LIBRARY)
//                .join(BOOK).on(BOOK.BOOK_ID.eq(PERSONAL_LIBRARY.BOOK_ID))
//                // giả sử có bảng read_progress(account_id, book_id, last_read_chapter, last_read_at)
//                .leftJoin(READ_PROGRESS).on(
//                READ_PROGRESS.ACCOUNT_ID.eq(PERSONAL_LIBRARY.ACCOUNT_ID)
//                        .and(READ_PROGRESS.BOOK_ID.eq(PERSONAL_LIBRARY.BOOK_ID))
//        )
//                .where(PERSONAL_LIBRARY.ACCOUNT_ID.eq(accountId))
//                .and(prefixMatch.or(containsMatch))
//                .orderBy(
//                        DSL.when(prefixMatch, 0).otherwise(1), // ưu tiên prefix match
//                        BOOK.TITLE.asc()
//                )
//                .fetchInto(PersonalLibraryBookDTO.class);
//    }

}
