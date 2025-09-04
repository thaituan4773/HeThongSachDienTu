package com.ddtt.repositories;

import com.ddtt.dtos.RatingDTO;
import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.core.annotation.Blocking;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import static com.ddtt.jooq.generated.tables.Rating.RATING;
import static com.ddtt.jooq.generated.tables.Book.BOOK;
import java.util.Optional;

@Singleton
@Blocking
@RequiredArgsConstructor
public class RatingRepository {

    private final DSLContext dsl;

    @Cacheable("rating-global-avg")
    public double getGlobalAvg() {
        Double avg = dsl.select(DSL.avg(RATING.SCORE))
                .from(RATING)
                .fetchOneInto(Double.class);
        System.out.print(avg);
        return avg != null ? avg : 0.0;
    }

    public Optional<RatingDTO> upsertRating(RatingDTO dto, int accountId) {
        RatingDTO result = dsl.insertInto(RATING)
                .columns(RATING.BOOK_ID, RATING.ACCOUNT_ID, RATING.SCORE)
                .select(dsl.select(BOOK.BOOK_ID, DSL.val(accountId), DSL.val(dto.getScore()))
                        .from(BOOK)
                        .where(BOOK.BOOK_ID.eq(dto.getBookId())
                                .and(BOOK.AUTHOR_ACCOUNT_ID.ne(accountId))))
                .onConflict(RATING.BOOK_ID, RATING.ACCOUNT_ID)
                .doUpdate()
                .set(RATING.SCORE, dto.getScore())
                .returning(RATING.BOOK_ID, RATING.SCORE)
                .fetchOne(record -> new RatingDTO(
                record.get(RATING.BOOK_ID),
                record.get(RATING.SCORE)
        ));

        return Optional.ofNullable(result);
    }
    
    public boolean deleteRating(int accountId, int bookId) {
        int affectedRows = dsl.deleteFrom(RATING)
                .where(RATING.ACCOUNT_ID.eq(accountId)
                        .and(RATING.BOOK_ID.eq(bookId)))
                .execute();

        return affectedRows > 0;
    }
}
