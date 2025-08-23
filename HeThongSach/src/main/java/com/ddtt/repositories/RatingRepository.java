package com.ddtt.repositories;

import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.core.annotation.Blocking;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import static com.ddtt.jooq.generated.tables.Rating.RATING;

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
}
