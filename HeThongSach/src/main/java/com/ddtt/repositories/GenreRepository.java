package com.ddtt.repositories;

import io.micronaut.core.annotation.Blocking;
import jakarta.inject.Singleton;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import static com.ddtt.jooq.generated.tables.Book.BOOK;
import static com.ddtt.jooq.generated.tables.BookView.BOOK_VIEW;
import static com.ddtt.jooq.generated.tables.Genre.GENRE;
import java.time.OffsetDateTime;
import java.util.List;
import org.jooq.impl.DSL;

@Singleton
@Blocking
@RequiredArgsConstructor
public class GenreRepository {

    private final DSLContext dsl;

    public Map<Integer, Integer> countGenresByUser(int userId, int days) {
        return dsl.select(BOOK.GENRE_ID, DSL.count())
                .from(BOOK_VIEW)
                .join(BOOK).on(BOOK_VIEW.BOOK_ID.eq(BOOK.BOOK_ID))
                .where(BOOK_VIEW.ACCOUNT_ID.eq(userId)
                        .and(BOOK_VIEW.VIEWED_AT.ge(OffsetDateTime.now().minusDays(days))))
                .groupBy(BOOK.GENRE_ID)
                .fetchMap(BOOK.GENRE_ID, DSL.count());
    }

    public List<Integer> getAllGenreId() {
        return dsl.select(GENRE.GENRE_ID)
                .from(GENRE)
                .fetch(GENRE.GENRE_ID)
                .stream()
                .map(Short::intValue) // chuyển sang Integer
                .toList();
    }

    public List<Integer> pickRandomGenreIds(int count) {
        return dsl.select(GENRE.GENRE_ID)
                .from(GENRE)
                .orderBy(DSL.rand())
                .limit(count)
                .fetchInto(Integer.class);
    }
    
    public String getGenreName(int genreId) {
        short id = (short) genreId;
        return dsl.select(GENRE.NAME)
                .from(GENRE)
                .where(GENRE.GENRE_ID.eq(id))
                .fetchOneInto(String.class);
    }
}
