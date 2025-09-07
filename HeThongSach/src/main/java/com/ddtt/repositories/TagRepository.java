package com.ddtt.repositories;

import com.ddtt.dtos.TagDTO;
import com.ddtt.exceptions.DuplicateException;
import static com.ddtt.jooq.generated.tables.Book.BOOK;
import static com.ddtt.jooq.generated.tables.BookTag.BOOK_TAG;
import static com.ddtt.jooq.generated.tables.BookView.BOOK_VIEW;
import static com.ddtt.jooq.generated.tables.Tag.TAG;
import com.ddtt.jooq.generated.tables.records.TagRecord;
import io.micronaut.core.annotation.Blocking;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

@Singleton
@Blocking
@RequiredArgsConstructor
public class TagRepository {

    private final DSLContext dsl;

    public Map<Integer, Integer> countTagsByUser(int userId, int days) {
        return dsl.select(BOOK_TAG.TAG_ID, DSL.count())
                .from(BOOK_VIEW)
                .join(BOOK).on(BOOK_VIEW.BOOK_ID.eq(BOOK.BOOK_ID))
                .join(BOOK_TAG).on(BOOK.BOOK_ID.eq(BOOK_TAG.BOOK_ID))
                .where(BOOK_VIEW.ACCOUNT_ID.eq(userId)
                        .and(BOOK_VIEW.VIEWED_AT.ge(OffsetDateTime.now().minusDays(days))))
                .groupBy(BOOK_TAG.TAG_ID)
                .fetchMap(BOOK_TAG.TAG_ID, DSL.count());
    }

    public List<Integer> getAllTagIds() {
        return dsl.select(TAG.TAG_ID)
                .from(TAG)
                .fetch(TAG.TAG_ID);
    }

    public List<TagDTO> getAllTags() {
        return dsl.select(
                TAG.TAG_ID.as("tagId"),
                TAG.NAME.as("tagName")
        )
                .from(TAG)
                .fetchInto(TagDTO.class);
    }

    public List<String> suggestTagNames(String prefix, int limit) {
        if (prefix == null || prefix.isBlank()) {
            return List.of();
        }

        String pattern = prefix.trim() + "%";

        return dsl.select(TAG.NAME)
                .from(TAG)
                .where(TAG.NAME.likeIgnoreCase(pattern))
                .orderBy(TAG.USAGE.desc())
                .limit(limit)
                .fetchInto(String.class);
    }

    public void insertTagsIfNotExists(int bookId, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }
        // Insert tags, bỏ qua nếu đã tồn tại
        tagNames.forEach(name
                -> dsl.insertInto(TAG)
                        .set(TAG.NAME, name)
                        .onConflictDoNothing()
                        .execute()
        );

        // 2) Insert vào book_tag
        dsl.insertInto(BOOK_TAG, BOOK_TAG.BOOK_ID, BOOK_TAG.TAG_ID)
                .select(
                        dsl.select(DSL.val(bookId), TAG.TAG_ID)
                                .from(TAG)
                                .where(TAG.NAME.in(tagNames))
                )
                .onConflictDoNothing() // tránh duplicate trong book_tag
                .execute();
    }

    public void deleteTagsNotIn(int bookId, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            // Nếu danh sách mới trống, xóa tất cả tag của sách
            dsl.deleteFrom(BOOK_TAG)
                    .where(BOOK_TAG.BOOK_ID.eq(bookId))
                    .execute();
            return;
        }

        // Xóa các tag cũ không có trong danh sách mới
        dsl.deleteFrom(BOOK_TAG)
                .where(BOOK_TAG.BOOK_ID.eq(bookId))
                .and(BOOK_TAG.TAG_ID.notIn(
                        dsl.select(TAG.TAG_ID)
                                .from(TAG)
                                .where(TAG.NAME.in(tagNames))
                ))
                .execute();
    }
}
