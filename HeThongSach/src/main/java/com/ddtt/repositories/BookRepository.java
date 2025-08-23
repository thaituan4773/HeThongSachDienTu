package com.ddtt.repositories;

import com.ddtt.dtos.BookDTO;
import com.ddtt.dtos.BookDetailDTO;
import com.ddtt.dtos.BookSummaryDTO;
import com.ddtt.dtos.ChapterDTO;
import com.ddtt.dtos.PageResponseDTO;
import static com.ddtt.jooq.generated.tables.Book.BOOK;
import static com.ddtt.jooq.generated.tables.BookView.BOOK_VIEW;
import static com.ddtt.jooq.generated.tables.BookTag.BOOK_TAG;
import static com.ddtt.jooq.generated.tables.Rating.RATING;
import static com.ddtt.jooq.generated.tables.Donation.DONATION;
import static com.ddtt.jooq.generated.tables.Genre.GENRE;
import static com.ddtt.jooq.generated.tables.Chapter.CHAPTER;
import io.micronaut.core.annotation.Blocking;
import jakarta.inject.Singleton;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;

@Singleton
@Blocking
@RequiredArgsConstructor
public class BookRepository {

    private final DSLContext dsl;
    private final RatingRepository ratingRepository;

    public List<BookDTO> findAllBooks() {
        return dsl.selectFrom(BOOK).fetch(record -> new BookDTO(
                record.getBookId(),
                record.getTitle(),
                null
        ));
    }

    public List<BookDTO> findTrendingBooks(int days, int limit) {
        OffsetDateTime since = OffsetDateTime.now().minusDays(days);

        return dsl.select(
                BOOK.BOOK_ID,
                BOOK.TITLE,
                BOOK.COVER_IMAGE_URL,
                DSL.count(BOOK_VIEW.ACCOUNT_ID).as("viewCount")
        )
                .from(BOOK)
                .join(BOOK_VIEW).on(BOOK.BOOK_ID.eq(BOOK_VIEW.BOOK_ID))
                .where(BOOK_VIEW.VIEWED_AT.ge(since))
                .groupBy(BOOK.BOOK_ID, BOOK.TITLE, BOOK.COVER_IMAGE_URL)
                .orderBy(DSL.field("viewCount").desc())
                .limit(limit)
                .fetchInto(BookDTO.class);
    }

    // Lấy ngẫu nhiên một cuốn sách theo tagId, nhưng loại bỏ các sách mà userId đã xem
    public Optional<BookDTO> findRandomBookByTagExcludingUser(int tagId, int userId) {

        // Lấy danh sách BOOK_ID mà user đã xem từ bảng BOOK_VIEW
        // Để loại trừ các sách đó khi query chính
        var viewedBooks = DSL.select(BOOK_VIEW.BOOK_ID)
                .from(BOOK_VIEW)
                .where(BOOK_VIEW.ACCOUNT_ID.eq(userId));

        return dsl.select(BOOK.BOOK_ID, BOOK.TITLE, BOOK.COVER_IMAGE_URL)
                .from(BOOK)
                .join(BOOK_TAG).on(BOOK.BOOK_ID.eq(BOOK_TAG.BOOK_ID))
                .where(BOOK_TAG.TAG_ID.eq(tagId))
                .and(BOOK.BOOK_ID.notIn(viewedBooks))
                .and(BOOK.DELETED_AT.isNull())
                .orderBy(DSL.field("random()"))
                .limit(1)
                .fetchOptionalInto(BookDTO.class);
    }

    public Optional<BookDTO> findRandomBookByGenreExcludingUser(int genreId, int userId) {
        var viewedBooks = DSL.select(BOOK_VIEW.BOOK_ID)
                .from(BOOK_VIEW)
                .where(BOOK_VIEW.ACCOUNT_ID.eq(userId));

        return dsl.select(BOOK.BOOK_ID, BOOK.TITLE, BOOK.COVER_IMAGE_URL)
                .from(BOOK)
                .where(BOOK.GENRE_ID.eq(genreId))
                .and(BOOK.BOOK_ID.notIn(viewedBooks))
                .and(BOOK.DELETED_AT.isNull())
                .orderBy(DSL.field("random()"))
                .limit(1)
                .fetchOptionalInto(BookDTO.class);
    }

    public List<BookDTO> findNewestBooks(int limit) {
        return dsl.select(
                BOOK.BOOK_ID,
                BOOK.TITLE,
                BOOK.COVER_IMAGE_URL
        )
                .from(BOOK)
                .where(BOOK.DELETED_AT.isNull())
                .orderBy(BOOK.CREATED_AT.desc())
                .limit(limit)
                .fetchInto(BookDTO.class);
    }

    public List<BookDTO> findBooksByGenre(int genreId, int limit) {
        return dsl.select(
                BOOK.BOOK_ID,
                BOOK.TITLE,
                BOOK.COVER_IMAGE_URL
        )
                .from(BOOK)
                .where(BOOK.GENRE_ID.eq(genreId))
                .orderBy(BOOK.PUBLISHED_DATE.desc())
                .limit(limit)
                .fetch(record -> new BookDTO(
                record.get(BOOK.BOOK_ID),
                record.get(BOOK.TITLE),
                record.get(BOOK.COVER_IMAGE_URL)
        ));
    }

    // các sub query
    private Table<?> viewsAgg() {
        return dsl.select(BOOK_VIEW.BOOK_ID.as("v_book_id"),
                DSL.count().as("totalView"))
                .from(BOOK_VIEW)
                .groupBy(BOOK_VIEW.BOOK_ID)
                .asTable("v");
    }

    private Table<?> ratingsAgg() {
        return dsl.select(RATING.BOOK_ID.as("r_book_id"),
                DSL.count().as("totalRating"),
                DSL.avg(RATING.SCORE).as("avgRating"))
                .from(RATING)
                .groupBy(RATING.BOOK_ID)
                .asTable("r");
    }

    private Table<?> donationsAgg() {
        return dsl.select(DONATION.BOOK_ID.as("d_book_id"),
                DSL.sum(DONATION.COIN_AMOUNT).as("totalDonate"))
                .from(DONATION)
                .groupBy(DONATION.BOOK_ID)
                .asTable("d");
    }

    public List<BookDTO> findTopRatedBooks(int limit) {
        double globalAvg = ratingRepository.getGlobalAvg();
        final int m = 20;
        var rAgg = ratingsAgg();

        Field<Integer> totalRatingF = DSL.coalesce(rAgg.field("totalRating", Integer.class), DSL.inline(0)).as("totalRating");
        Field<Double> avgRatingF = DSL.coalesce(rAgg.field("avgRating", Double.class), DSL.inline(0.0)).as("avgRating");

        return dsl.select(
                BOOK.BOOK_ID,
                BOOK.TITLE,
                BOOK.COVER_IMAGE_URL
        )
                .from(BOOK)
                .leftJoin(rAgg).on(BOOK.BOOK_ID.eq(rAgg.field("r_book_id", Integer.class)))
                .orderBy(
                        // bayesScore = (v*R + m*C) / (v + m) 
                        // v = totalRatingF
                        // R = avgRatingF
                        // C = globalAvg
                        // m = hằng số
                        totalRatingF.mul(avgRatingF)
                                .plus(DSL.inline((double) m).mul(DSL.inline(globalAvg)))
                                .divide(totalRatingF.plus(DSL.inline((double) m))).desc(),
                        // Tie breakers
                        totalRatingF.desc(),
                        avgRatingF.desc(),
                        BOOK.CREATED_AT.desc(),
                        BOOK.BOOK_ID.desc()
                )
                .limit(limit)
                .fetch(record -> new BookDTO(
                record.get(BOOK.BOOK_ID),
                record.get(BOOK.TITLE),
                record.get(BOOK.COVER_IMAGE_URL)
        ));
    }

    public BookDetailDTO findBookDetail(int bookId) {

        var vAgg = viewsAgg();
        var rAgg = ratingsAgg();
        var dAgg = donationsAgg();

        BookDetailDTO bookDetail = dsl.select(
                BOOK.TITLE.as("bookName"),
                GENRE.NAME.as("genreName"),
                BOOK.AUTHOR_ACCOUNT_ID.as("authorID"),
                BOOK.DESCRIPTION,
                DSL.coalesce(vAgg.field("totalView", Integer.class), DSL.inline(0)).as("totalView"),
                DSL.coalesce(rAgg.field("totalRating", Integer.class), DSL.inline(0)).as("totalRating"),
                DSL.coalesce(rAgg.field("avgRating", Double.class), DSL.inline(0.0)).as("avgRating"),
                DSL.coalesce(dAgg.field("totalDonate", Long.class), DSL.inline(0L)).as("totalDonate")
        )
                .from(BOOK)
                .join(GENRE).on(BOOK.GENRE_ID.eq(GENRE.GENRE_ID.cast(Integer.class)))
                .leftJoin(vAgg).on(BOOK.BOOK_ID.eq(vAgg.field("v_book_id", Integer.class)))
                .leftJoin(rAgg).on(BOOK.BOOK_ID.eq(rAgg.field("r_book_id", Integer.class)))
                .leftJoin(dAgg).on(BOOK.BOOK_ID.eq(dAgg.field("d_book_id", Integer.class)))
                .where(BOOK.BOOK_ID.eq(bookId).and(BOOK.DELETED_AT.isNull()))
                .fetchOneInto(BookDetailDTO.class);

        if (bookDetail == null) {
            return null;
        }

        List<ChapterDTO> chapters = dsl.select(
                CHAPTER.CHAPTER_ID,
                CHAPTER.TITLE,
                CHAPTER.POSITION.as("order"),
                CHAPTER.COIN_PRICE.as("coinPrice"),
                CHAPTER.CREATED_AT.as("createdDate")
        )
                .from(CHAPTER)
                .where(CHAPTER.BOOK_ID.eq(bookId))
                .orderBy(CHAPTER.POSITION.asc())
                .fetchInto(ChapterDTO.class);

        bookDetail.setChapters(chapters);
        return bookDetail;
    }

    // Tìm theo tên sách, description, tác giả
    public PageResponseDTO<BookSummaryDTO> searchBooks(String kw, int page, int size) {
        int offset = page * size;

        String trimmed = kw == null ? "" : kw.trim();
        boolean hasKw = !trimmed.isEmpty();

        Condition condition = BOOK.DELETED_AT.isNull();
        Field<Integer> matchScore = DSL.inline(0);

        if (hasKw) {
            Field<String> pattern = DSL.val("%" + trimmed + "%");
            var titleCond = BOOK.TITLE.likeIgnoreCase(pattern);
            var descCond = BOOK.DESCRIPTION.likeIgnoreCase(pattern);
            
            // Tính độ phù hợp
            // Tựa đề + 10
            // Mô tả + 5
            Field<Integer> titleScore = DSL.when(titleCond, DSL.inline(10)).otherwise(0);
            Field<Integer> descScore = DSL.when(descCond, DSL.inline(5)).otherwise(0);
            matchScore = titleScore.add(descScore);
            condition = condition.and(titleCond.or(descCond));
        }

        List<BookSummaryDTO> items = dsl.select(
                BOOK.BOOK_ID.as("bookId"),
                BOOK.TITLE.as("title"),
                BOOK.COVER_IMAGE_URL.as("coverImageURL")
        )
                .from(BOOK)
                .where(condition)
                .orderBy(
                        matchScore.desc(),
                        BOOK.CREATED_AT.desc(),
                        BOOK.BOOK_ID.desc()
                )
                .limit(size)
                .offset(offset)
                .fetchInto(BookSummaryDTO.class);

        long total = dsl.fetchCount(
                dsl.select(BOOK.BOOK_ID)
                        .from(BOOK)
                        .where(condition)
        );

        int totalPages = (int) Math.ceil((double) total / size);
        return new PageResponseDTO<>(total, page, size, totalPages, items);
    }

}
