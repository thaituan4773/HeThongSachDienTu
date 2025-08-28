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
        return avg != null ? avg : 0.0;
    }

    public Optional<RatingDTO> upsertRating(RatingDTO dto) {
        RatingDTO result = dsl.insertInto(RATING)
                .columns(RATING.BOOK_ID, RATING.ACCOUNT_ID, RATING.SCORE)
                .select(dsl.select(BOOK.BOOK_ID, DSL.val(dto.getAccountId()), DSL.val(dto.getScore()))
                        .from(BOOK)
                        .where(BOOK.BOOK_ID.eq(dto.getBookId())
                                .and(BOOK.AUTHOR_ACCOUNT_ID.ne(dto.getAccountId()))))
                .onConflict(RATING.BOOK_ID, RATING.ACCOUNT_ID)
                .doUpdate()
                .set(RATING.SCORE, dto.getScore())
                .returning(RATING.BOOK_ID, RATING.ACCOUNT_ID, RATING.SCORE)
                .fetchOneInto(RatingDTO.class);

        return Optional.ofNullable(result);
    }
}
