package com.ddtt.controllers;

import com.ddtt.dtos.CategoryDTO;
import com.ddtt.repositories.RatingRepository;
import com.ddtt.services.BookService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.authentication.Authentication;
import java.util.ArrayList;
import static com.ddtt.jooq.generated.tables.Book.BOOK;
import static com.ddtt.jooq.generated.tables.Rating.RATING;
import com.ddtt.repository.conditions.BookConditions;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;

@Controller("/api")
@RequiredArgsConstructor
public class Test {

    private final BookService bookService;
    private final RatingRepository ratingRepository;
    private final DSLContext dsl;

    @Get("/test")
    public HttpResponse<List<CategoryDTO>> explore(Authentication authentication) {
        debugTopRatedBooks();
        return HttpResponse.ok();
    }

    private Table<?> ratingsSub() {
        return dsl.select(
                RATING.BOOK_ID.as("r_book_id"),
                DSL.count().as("totalRating"),
                DSL.avg(RATING.SCORE).as("avgRating"))
                .from(RATING)
                .groupBy(RATING.BOOK_ID)
                .asTable("r");
    }

    public void debugTopRatedBooks() {
        double globalAvg = ratingRepository.getGlobalAvg();
        final int m = 20;

        var rSub = ratingsSub();

        Field<Integer> totalRatingF = DSL.coalesce(rSub.field("totalRating", Integer.class), DSL.inline(0)).as("totalRating");
        Field<Double> avgRatingF = DSL.coalesce(rSub.field("avgRating", Double.class), DSL.inline(0.0)).as("avgRating");
        Field<Double> vDouble = totalRatingF.cast(Double.class);
        Field<Double> mParam = DSL.inline((double) m);
        Field<Double> cParam = DSL.val(globalAvg);

        // bayesScore = (v*R + m*C) / (v + m)
        Field<Double> bayesScore = DSL
                .coalesce(
                        vDouble.mul(avgRatingF)
                                .plus(mParam.mul(cParam))
                                .divide(vDouble.plus(mParam)),
                        cParam
                ).as("bayesScore");

        var query = dsl.select(
                BOOK.BOOK_ID.as("bookId"),
                BOOK.TITLE.as("title"),
                BOOK.COVER_IMAGE_URL.as("coverImageURL"),
                totalRatingF,
                avgRatingF,
                bayesScore
        )
                .from(BOOK)
                .leftJoin(rSub).on(BOOK.BOOK_ID.eq(rSub.field("r_book_id", Integer.class)))
                .where(BookConditions.discoverableStatus())
                .orderBy(
                        bayesScore.desc(),
                        totalRatingF.desc(),
                        avgRatingF.desc(),
                        BOOK.CREATED_AT.desc(),
                        BOOK.BOOK_ID.desc()
                );

        System.out.println("SQL = " + query.getSQL());
        System.out.println("BIND VALUES = " + query.getBindValues());

        var result = query.fetch();
        result.forEach(r -> {
            System.out.printf(
                    "BookID=%d | Title=%s | Total=%d | Avg=%.2f | Bayes=%.4f%n",
                    r.get("bookId", Integer.class),
                    r.get("title", String.class),
                    r.get("totalRating", Integer.class),
                    r.get("avgRating", Double.class),
                    r.get("bayesScore", Double.class)
            );
        });
    }
}
