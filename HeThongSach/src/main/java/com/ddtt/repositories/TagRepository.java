package com.ddtt.repositories;

import static com.ddtt.jooq.generated.tables.Book.BOOK;
import static com.ddtt.jooq.generated.tables.BookTag.BOOK_TAG;
import static com.ddtt.jooq.generated.tables.BookView.BOOK_VIEW;
import static com.ddtt.jooq.generated.tables.Tag.TAG;
import io.micronaut.core.annotation.Blocking;
import jakarta.inject.Singleton;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
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
    public List<Integer> getAllTagIds(){
        return dsl.select(TAG.TAG_ID)
                .from(TAG)
                .fetch(TAG.TAG_ID);
    }

}
