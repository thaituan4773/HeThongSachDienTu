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
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import org.jooq.Field;
import org.jooq.SortField;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

@Singleton
@Blocking
@RequiredArgsConstructor
public class PersonalLibraryRepository {

    private final DSLContext dsl;

    public PageResponseDTO<PersonalLibraryBookDTO> getPersonalLibrary(
            int accountId,
            String kw, // Nếu null hoặc rỗng = lấy tất cả
            int page,
            int size,
            String sortBy,
            boolean desc,
            Boolean unreaded // null = tất cả, true = unread, false = read
    ) {
        int offset = (page - 1) * size;
        String trimmed = kw != null ? kw.trim() : null;

        // Subquery đếm tổng chương
        var chapterCount = dsl.select(
                CHAPTER.BOOK_ID,
                DSL.count().as("totalChapters")
        )
                .from(CHAPTER)
                .where(ChapterConditions.isPublished())
                .groupBy(CHAPTER.BOOK_ID)
                .asTable("chapterCount");

        Field<Integer> readChaptersField = DSL.field(
                "bit_count({0})", Integer.class, READING_PROGRESS.READ_CHAPTERS_BITMAP
        );

        Field<Integer> totalChaptersField = DSL.coalesce(
                chapterCount.field("totalChapters", Integer.class),
                DSL.inline(0)
        );

        Field<Integer> unreadExpr = totalChaptersField
                .minus(DSL.coalesce(readChaptersField, DSL.inline(0)));

        Field<Integer> unreadChaptersField = unreadExpr.as("unreadChapters");
        // Base query
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
                .leftJoin(READING_PROGRESS).on(
                READING_PROGRESS.ACCOUNT_ID.eq(PERSONAL_LIBRARY.ACCOUNT_ID)
                        .and(READING_PROGRESS.BOOK_ID.eq(BOOK.BOOK_ID))
        )
                .where(PERSONAL_LIBRARY.ACCOUNT_ID.eq(accountId));

        // Search theo title nếu có keyword
        if (trimmed != null && !trimmed.isEmpty()) {
            base = base.and(BOOK.TITLE.likeIgnoreCase("%" + trimmed + "%"));
        }

        // Filter unreaded
        if (Boolean.TRUE.equals(unreaded)) {
            base = base.and(unreadExpr.gt(0));
        } else if (Boolean.FALSE.equals(unreaded)) {
            base = base.and(unreadExpr.eq(0));
        }

        // Sort
        SortField<?> sortField = switch (sortBy != null ? sortBy : "title") {
            case "unreadChapters" ->
                desc ? unreadExpr.desc() : unreadExpr.asc();
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

        long total = (long) dsl.fetchCount(base);
        int totalPages = (int) Math.ceil((double) total / size);

        return new PageResponseDTO<>(total, page, size, totalPages, items);
    }

    @Transactional
    public void addBookToLibrary(int accountId, int bookId) {
        // Insert mới
        try {
            dsl.insertInto(PERSONAL_LIBRARY)
                    .set(PERSONAL_LIBRARY.ACCOUNT_ID, accountId)
                    .set(PERSONAL_LIBRARY.BOOK_ID, bookId)
                    .set(PERSONAL_LIBRARY.FOLLOWED_AT, OffsetDateTime.now())
                    .execute();
        } catch (DataAccessException e) {
            if(DuplicateException.isDuplicate(e)){
                throw new DuplicateException("Sách " + bookId + " đã tồn tại trong account " + accountId);
            }
            throw e;
        }
    }

    public boolean removeBookFromLibrary(int accountId, int bookId) {
        int affectedRows = dsl.deleteFrom(PERSONAL_LIBRARY)
                .where(PERSONAL_LIBRARY.ACCOUNT_ID.eq(accountId))
                .and(PERSONAL_LIBRARY.BOOK_ID.eq(bookId))
                .execute();
        return affectedRows > 0;
    }

}
