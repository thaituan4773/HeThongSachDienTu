package com.ddtt.repositories;

import com.ddtt.dtos.BookCreateDTO;
import com.ddtt.dtos.BookDTO;
import com.ddtt.dtos.BookDetailDTO;
import com.ddtt.dtos.BookSummaryDTO;
import com.ddtt.dtos.ChapterOverviewDTO;
import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.dtos.TagDTO;
import static com.ddtt.jooq.generated.tables.Book.BOOK;
import static com.ddtt.jooq.generated.tables.BookView.BOOK_VIEW;
import static com.ddtt.jooq.generated.tables.Rating.RATING;
import static com.ddtt.jooq.generated.tables.Donation.DONATION;
import static com.ddtt.jooq.generated.tables.Genre.GENRE;
import static com.ddtt.jooq.generated.tables.Tag.TAG;
import static com.ddtt.jooq.generated.tables.BookTag.BOOK_TAG;
import static com.ddtt.jooq.generated.tables.Account.ACCOUNT;
import com.ddtt.repository.conditions.BookConditions;
import io.micronaut.core.annotation.Blocking;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.impl.DSL;

@Singleton
@Blocking
@RequiredArgsConstructor
public class BookRepository {

    private final DSLContext dsl;
    private final RatingRepository ratingRepository;
    private final ChapterRepository chapterRepository;

    public List<BookDTO> findAllBooks() {
        return dsl.selectFrom(BOOK)
                .where(BookConditions.discoverableStatus())
                .fetch(record -> new BookDTO(
                record.getBookId(),
                record.getTitle(),
                null
        ));
    }

    public List<BookDTO> findTrendingBooks(int days, int limit) {
        OffsetDateTime since = OffsetDateTime.now().minusDays(days);
        return dsl.select(
                BOOK.BOOK_ID.as("bookId"),
                BOOK.TITLE.as("title"),
                BOOK.COVER_IMAGE_URL.as("coverImageURL")
        )
                .from(BOOK)
                .join(BOOK_VIEW).on(BOOK.BOOK_ID.eq(BOOK_VIEW.BOOK_ID))
                .where(BOOK_VIEW.VIEWED_AT.ge(since))
                .and(BookConditions.discoverableStatus())
                .groupBy(BOOK.BOOK_ID, BOOK.TITLE, BOOK.COVER_IMAGE_URL)
                .orderBy(DSL.count().desc())
                .limit(limit)
                .fetchInto(BookDTO.class);   // <-- chỉ cần thế này
    }

    // Lấy ngẫu nhiên một cuốn sách theo tagId, nhưng loại bỏ các sách mà userId đã xem
    public Optional<BookDTO> findRandomBookByTagExcludingUser(int tagId, int userId) {

        var viewedBooks = DSL.select(BOOK_VIEW.BOOK_ID)
                .from(BOOK_VIEW)
                .where(BOOK_VIEW.ACCOUNT_ID.eq(userId));

        return dsl.select(BOOK.BOOK_ID, BOOK.TITLE, BOOK.COVER_IMAGE_URL)
                .from(BOOK)
                .join(BOOK_TAG).on(BOOK.BOOK_ID.eq(BOOK_TAG.BOOK_ID))
                .where(BOOK_TAG.TAG_ID.eq(tagId))
                .and(BOOK.BOOK_ID.notIn(viewedBooks))
                .and(BookConditions.discoverableStatus())
                .orderBy(DSL.field("random()"))
                .limit(1)
                .fetchOptionalInto(BookDTO.class);
    }

    public Optional<BookDTO> findRandomBookByGenreExcludingUser(int genreId, int userId) {
        var viewedBooks = DSL.select(BOOK_VIEW.BOOK_ID)
                .from(BOOK_VIEW)
                .where(BOOK_VIEW.ACCOUNT_ID.eq(userId));

        return dsl.select(
                BOOK.BOOK_ID.as("bookId"),
                BOOK.TITLE.as("title"),
                BOOK.COVER_IMAGE_URL.as("coverImageURL")
        )
                .from(BOOK)
                .where(BOOK.GENRE_ID.eq(genreId))
                .and(BOOK.BOOK_ID.notIn(viewedBooks))
                .and(BookConditions.discoverableStatus())
                .orderBy(DSL.field("random()"))
                .limit(1)
                .fetchOptionalInto(BookDTO.class);
    }

    public List<BookDTO> findNewestBooks(int limit) {
        System.out.println("Loading books for newest from DB");
        return dsl.select(
                BOOK.BOOK_ID.as("bookId"),
                BOOK.TITLE.as("title"),
                BOOK.COVER_IMAGE_URL.as("coverImageURL")
        )
                .from(BOOK)
                .where(BookConditions.discoverableStatus())
                .orderBy(BOOK.CREATED_AT.desc())
                .limit(limit)
                .fetchInto(BookDTO.class);
    }

    public List<BookDTO> findBooksByGenre(int genreId, int limit) {
        return dsl.select(
                BOOK.BOOK_ID.as("bookId"),
                BOOK.TITLE.as("title"),
                BOOK.COVER_IMAGE_URL.as("coverImageURL")
        )
                .from(BOOK)
                .where(BOOK.GENRE_ID.eq(genreId))
                .and(BookConditions.discoverableStatus())
                .orderBy(BOOK.CREATED_AT.desc())
                .limit(limit)
                .fetchInto(BookDTO.class);
    }

    // Subquery
    private Table<?> viewsSub() {
        return dsl.select(
                BOOK_VIEW.BOOK_ID.as("v_book_id"),
                DSL.count().as("totalView"))
                .from(BOOK_VIEW)
                .groupBy(BOOK_VIEW.BOOK_ID)
                .asTable("v");
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

    private Table<?> donationsSub() {
        return dsl.select(
                DONATION.BOOK_ID.as("d_book_id"),
                DSL.sum(DONATION.COIN_AMOUNT).as("totalDonate"))
                .from(DONATION)
                .groupBy(DONATION.BOOK_ID)
                .asTable("d");
    }

    public List<BookDTO> findTopRatedBooks(int limit) {
        double globalAvg = ratingRepository.getGlobalAvg();
        final int m = 20;

        var rSub = ratingsSub();

        Field<Integer> totalRatingF = DSL.coalesce(rSub.field("totalRating", Integer.class), DSL.inline(0)).as("totalRating");
        Field<Double> avgRatingF = DSL.coalesce(rSub.field("avgRating", Double.class), DSL.inline(0.0)).as("avgRating");
        Field<Double> vDouble = totalRatingF.cast(Double.class);
        Field<Double> mParam = DSL.inline((double) m);
        Field<Double> cParam = DSL.val(globalAvg);

        // bayesScore = (v*R + m*C) / (v + m) 
        // v = totalRatingF
        // R = avgRatingF
        // C = globalAvg
        // m = hằng số
        Field<Double> bayesScore = vDouble.mul(avgRatingF)
                .plus(mParam.mul(cParam))
                .divide(vDouble.plus(mParam));

        return dsl.select(
                BOOK.BOOK_ID.as("bookId"),
                BOOK.TITLE.as("title"),
                BOOK.COVER_IMAGE_URL.as("coverImageURL")
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
                )
                .limit(limit)
                .fetchInto(BookDTO.class);
    }

    public BookDetailDTO getBookDetail(int bookId, int accountId) {

        var vSub = viewsSub();
        var rSub = ratingsSub();
        var dSub = donationsSub();

        BookDetailDTO bookDetail = dsl.select(
                BOOK.TITLE.as("bookName"),
                GENRE.NAME.as("genre"),
                BOOK.COVER_IMAGE_URL.as("coverImageURL"),
                ACCOUNT.DISPLAY_NAME.as("authorName"),
                BOOK.AUTHOR_ACCOUNT_ID.as("authorID"),
                BOOK.STATUS.as("status"),
                BOOK.DESCRIPTION.as("description"),
                DSL.coalesce(vSub.field("totalView", Integer.class), DSL.inline(0)).as("totalView"),
                DSL.coalesce(rSub.field("totalRating", Integer.class), DSL.inline(0)).as("totalRating"),
                DSL.coalesce(rSub.field("avgRating", Double.class), DSL.inline(0.0)).as("avgRating"),
                DSL.coalesce(dSub.field("totalDonate", Long.class), DSL.inline(0L)).as("totalDonate"),
                DSL.when(BOOK.AUTHOR_ACCOUNT_ID.eq(accountId), true).otherwise(false).as("isAuthor")
        )
                .from(BOOK)
                .join(GENRE).on(BOOK.GENRE_ID.eq(GENRE.GENRE_ID))
                .join(ACCOUNT).on(BOOK.AUTHOR_ACCOUNT_ID.eq(ACCOUNT.ACCOUNT_ID))
                .leftJoin(vSub).on(BOOK.BOOK_ID.eq(vSub.field("v_book_id", Integer.class)))
                .leftJoin(rSub).on(BOOK.BOOK_ID.eq(rSub.field("r_book_id", Integer.class)))
                .leftJoin(dSub).on(BOOK.BOOK_ID.eq(dSub.field("d_book_id", Integer.class)))
                .where(BOOK.BOOK_ID.eq(bookId))
                .and(BOOK.DELETED_AT.isNull())
                .fetchOneInto(BookDetailDTO.class);

        if (bookDetail == null) {
            return null;
        }

        List<TagDTO> tags = dsl.select(
                TAG.TAG_ID.as("tagId"),
                TAG.NAME.as("tagName")
        )
                .from(TAG)
                .join(BOOK_TAG).on(TAG.TAG_ID.eq(BOOK_TAG.TAG_ID))
                .where(BOOK_TAG.BOOK_ID.eq(bookId))
                .fetchInto(TagDTO.class);

        bookDetail.setTags(tags);
        return bookDetail;
    }

    // Tìm theo tên sách, description
    public PageResponseDTO<BookSummaryDTO> searchBooks(String kw, int page, int size) {
        int offset = (page - 1) * size;

        String trimmed = kw == null ? "" : kw.trim();
        boolean hasKw = !trimmed.isEmpty();

        Condition condition = BookConditions.discoverableStatus();
        Field<Integer> matchScore = DSL.inline(0);

        if (hasKw) {
            Field<String> pattern = DSL.val("%" + trimmed + "%");
            var titleCond = BOOK.TITLE.likeIgnoreCase(pattern);
            var descCond = BOOK.DESCRIPTION.likeIgnoreCase(pattern);

            // mức độ phù hợp: title +10    description +5
            Field<Integer> titleScore = DSL.when(titleCond, DSL.inline(10)).otherwise(0);
            Field<Integer> descScore = DSL.when(descCond, DSL.inline(5)).otherwise(0);
            matchScore = titleScore.add(descScore);
            condition = condition.and(titleCond.or(descCond));
        }

        var v = viewsSub();
        var r = ratingsSub();
        Field<Integer> totalViewF = DSL.coalesce(v.field("totalView", Integer.class), DSL.inline(0)).as("totalView");
        Field<Integer> totalRatingF = DSL.coalesce(r.field("totalRating", Integer.class), DSL.inline(0)).as("totalRating");
        Field<Double> avgRatingF = DSL.coalesce(r.field("avgRating", Double.class), DSL.inline(0.0)).as("avgRating");

        List<BookSummaryDTO> items = dsl.select(
                BOOK.BOOK_ID.as("bookId"),
                BOOK.TITLE.as("title"),
                BOOK.COVER_IMAGE_URL.as("coverImageURL"),
                totalViewF,
                totalRatingF,
                avgRatingF
        )
                .from(BOOK)
                .leftJoin(v).on(BOOK.BOOK_ID.eq(v.field("v_book_id", Integer.class)))
                .leftJoin(r).on(BOOK.BOOK_ID.eq(r.field("r_book_id", Integer.class)))
                .where(condition)
                .orderBy(
                        matchScore.desc(),
                        BOOK.CREATED_AT.desc(),
                        BOOK.BOOK_ID.desc()
                )
                .limit(size)
                .offset(offset)
                .fetchInto(BookSummaryDTO.class);

        // Tính tổng số item và tổng số page, chỉ tính ở trang đầu
        Long total = null;
        Integer totalPages = null;
        if (page == 1) {
            long cnt = dsl.fetchCount(
                    dsl.select(BOOK.BOOK_ID)
                            .from(BOOK)
                            .where(condition)
            );
            total = cnt;
            totalPages = (int) Math.ceil((double) cnt / size);
        }

        return new PageResponseDTO<>(total, page, size, totalPages, items);
    }

    public PageResponseDTO<BookSummaryDTO> getBooksByGenrePaged(
            int genreId,
            int page,
            int size,
            String sort // "trending", "views", "topRated", "newest"
    ) {
        int offset = (page - 1) * size;

        Condition condition = BookConditions.discoverableStatus()
                .and(BOOK.GENRE_ID.eq(genreId));

        Table<?> vSub = viewsSub();
        Table<?> rSub = ratingsSub();

        Field<Integer> totalViewF = DSL.coalesce(vSub.field("totalView", Integer.class), DSL.inline(0)).as("totalView");
        Field<Integer> totalRatingF = DSL.coalesce(rSub.field("totalRating", Integer.class), DSL.inline(0)).as("totalRating");
        Field<Double> avgRatingF = DSL.coalesce(rSub.field("avgRating", Double.class), DSL.inline(0.0)).as("avgRating");

        SortField<?> orderField;
        Table<?> trendingSub = null;
        switch (sort != null ? sort.toLowerCase() : "") {
            case "views":
                orderField = totalViewF.desc();
                break;
            case "toprated":
                double globalAvg = ratingRepository.getGlobalAvg();
                final int m = 20;
                Field<Double> vDouble = totalRatingF.cast(Double.class);
                Field<Double> mParam = DSL.inline((double) m);
                Field<Double> cParam = DSL.val(globalAvg);
                Field<Double> avgRatingFDouble = avgRatingF;
                Field<Double> bayesScore = vDouble.mul(avgRatingFDouble)
                        .plus(mParam.mul(cParam))
                        .divide(vDouble.plus(mParam));
                orderField = bayesScore.desc();
                break;
            case "newest":
                orderField = BOOK.CREATED_AT.desc();
                break;
            case "trending":
            default:
                OffsetDateTime since = OffsetDateTime.now().minusDays(7);
                trendingSub = dsl.select(
                        BOOK_VIEW.BOOK_ID.as("t_book_id"),
                        DSL.count(BOOK_VIEW.ACCOUNT_ID).as("recentView")
                )
                        .from(BOOK_VIEW)
                        .where(BOOK_VIEW.VIEWED_AT.ge(since))
                        .groupBy(BOOK_VIEW.BOOK_ID)
                        .asTable("t");
                totalViewF = DSL.coalesce(trendingSub.field("recentView", Integer.class), DSL.inline(0)).as("totalView");
                orderField = totalViewF.desc();
                vSub = trendingSub;
                break;
        }

        List<BookSummaryDTO> items = dsl.select(
                BOOK.BOOK_ID.as("bookId"),
                BOOK.TITLE.as("title"),
                BOOK.COVER_IMAGE_URL.as("coverImageURL"),
                totalViewF,
                totalRatingF,
                avgRatingF
        )
                .from(BOOK)
                .leftJoin(vSub).on(BOOK.BOOK_ID.eq(
                vSub.field(vSub == trendingSub ? "t_book_id" : "v_book_id", Integer.class)))
                .leftJoin(rSub).on(BOOK.BOOK_ID.eq(rSub.field("r_book_id", Integer.class)))
                .where(condition)
                .orderBy(orderField, BOOK.BOOK_ID.desc())
                .limit(size)
                .offset(offset)
                .fetchInto(BookSummaryDTO.class);

        // chỉ tính total/totalPages ở page = 1
        Long total = null;
        Integer totalPages = null;
        if (page == 1) {
            long cnt = dsl.fetchCount(
                    dsl.select(BOOK.BOOK_ID)
                            .from(BOOK)
                            .where(condition)
            );
            total = cnt;
            totalPages = (int) Math.ceil((double) cnt / size);
        }

        return new PageResponseDTO<>(total, page, size, totalPages, items);
    }

    public PageResponseDTO<BookSummaryDTO> getBooksByAuthorPaged(int authorId, int page, int size, boolean isAuthor) {
        int offset = (page - 1) * size;
        Condition condition;
        if (isAuthor) {
            condition = BOOK.DELETED_AT.isNull().and(BOOK.AUTHOR_ACCOUNT_ID.eq(authorId));
        } else {
            condition = BookConditions.discoverableStatus().and(BOOK.AUTHOR_ACCOUNT_ID.eq(authorId));
        }

        Table<?> vSub = viewsSub();
        Table<?> rSub = ratingsSub();

        Field<Integer> totalViewF = DSL.coalesce(vSub.field("totalView", Integer.class), DSL.inline(0)).as("totalView");
        Field<Integer> totalRatingF = DSL.coalesce(rSub.field("totalRating", Integer.class), DSL.inline(0)).as("totalRating");
        Field<Double> avgRatingF = DSL.coalesce(rSub.field("avgRating", Double.class), DSL.inline(0.0)).as("avgRating");

        List<BookSummaryDTO> items = dsl.select(
                BOOK.BOOK_ID.as("bookId"),
                BOOK.TITLE.as("title"),
                BOOK.COVER_IMAGE_URL.as("coverImageURL"),
                totalViewF,
                totalRatingF,
                avgRatingF
        )
                .from(BOOK)
                .leftJoin(vSub).on(BOOK.BOOK_ID.eq(vSub.field("v_book_id", Integer.class)))
                .leftJoin(rSub).on(BOOK.BOOK_ID.eq(rSub.field("r_book_id", Integer.class)))
                .where(condition)
                .orderBy(BOOK.CREATED_AT.desc(), BOOK.BOOK_ID.desc())
                .limit(size)
                .offset(offset)
                .fetchInto(BookSummaryDTO.class);

        // chỉ tính total/totalPages ở page = 1
        Long total = null;
        Integer totalPages = null;
        if (page == 1) {
            long cnt = dsl.fetchCount(
                    dsl.select(BOOK.BOOK_ID)
                            .from(BOOK)
                            .where(condition)
            );
            total = cnt;
            totalPages = (int) Math.ceil((double) cnt / size);
        }

        return new PageResponseDTO<>(total, page, size, totalPages, items);
    }

    @Transactional
    public BookSummaryDTO createBook(BookCreateDTO dto, int authorId) {
        // Insert vào bảng BOOK
        var record = dsl.insertInto(BOOK)
                .set(BOOK.TITLE, dto.getTitle())
                .set(BOOK.DESCRIPTION, dto.getDescription())
                .set(BOOK.GENRE_ID, dto.getGenreId())
                .set(BOOK.COVER_IMAGE_URL, dto.getCoverImageURL())
                .set(BOOK.AUTHOR_ACCOUNT_ID, authorId)
                .returning(BOOK.BOOK_ID)
                .fetchOne();

        int bookId = record.getBookId();

        // Insert tags (nếu có)
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            for (Integer tagId : dto.getTagIds()) {
                dsl.insertInto(BOOK_TAG)
                        .set(BOOK_TAG.BOOK_ID, bookId)
                        .set(BOOK_TAG.TAG_ID, tagId)
                        .execute();
            }
        }
        BookSummaryDTO newBook = BookSummaryDTO.builder()
                .bookId(bookId)
                .title(dto.getTitle())
                .coverImageURL(dto.getCoverImageURL())
                .avgRating(0)
                .totalRating(0)
                .totalView(0)
                .build();
        return newBook;
    }
}
