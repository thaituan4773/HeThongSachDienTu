package com.ddtt.repositories;

import com.ddtt.dtos.BookInputDTO;
import com.ddtt.dtos.BookDTO;
import com.ddtt.dtos.BookFullDetailDTO;
import com.ddtt.dtos.BookSummaryAuthorDTO;
import com.ddtt.dtos.BookSummaryDTO;
import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.dtos.TagDTO;
import static com.ddtt.jooq.generated.tables.Book.BOOK;
import static com.ddtt.jooq.generated.tables.BookView.BOOK_VIEW;
import static com.ddtt.jooq.generated.tables.Rating.RATING;
import static com.ddtt.jooq.generated.tables.Genre.GENRE;
import static com.ddtt.jooq.generated.tables.Tag.TAG;
import static com.ddtt.jooq.generated.tables.BookTag.BOOK_TAG;
import static com.ddtt.jooq.generated.tables.Account.ACCOUNT;
import static com.ddtt.jooq.generated.tables.BookStats.BOOK_STATS;
import static com.ddtt.jooq.generated.tables.PersonalLibrary.PERSONAL_LIBRARY;
import static com.ddtt.jooq.generated.tables.ReadingProgress.READING_PROGRESS;
import static com.ddtt.jooq.generated.tables.Chapter.CHAPTER;
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
import org.jooq.UpdateQuery;
import org.jooq.impl.DSL;

@Singleton
@Blocking
@RequiredArgsConstructor
public class BookRepository {

    private final DSLContext dsl;
    private final RatingRepository ratingRepository;
    private final TagRepository tagRepository;
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

    public List<BookDTO> findTrendingBooks(int limit) {
        return dsl.select(
                BOOK.BOOK_ID.as("bookId"),
                BOOK.TITLE.as("title"),
                BOOK.COVER_IMAGE_URL.as("coverImageURL")
        )
                .from(BOOK)
                .join(BOOK_STATS).on(BOOK.BOOK_ID.eq(BOOK_STATS.BOOK_ID))
                .where(BookConditions.discoverableStatus())
                .orderBy(BOOK_STATS.TOTAL_VIEW_7D.desc())
                .limit(limit)
                .fetchInto(BookDTO.class);
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

    public List<BookDTO> findTopRatedBooks(int limit) {
        double globalAvg = ratingRepository.getGlobalAvg();
        final int m = 20;

        // topRatedScore = (v*R + m*C) / (v + m) 
        Field<Double> score = BOOK_STATS.TOTAL_RATING.cast(Double.class)
                .mul(BOOK_STATS.AVG_RATING)
                .plus(DSL.inline((double) m).mul(globalAvg))
                .divide(BOOK_STATS.TOTAL_RATING.cast(Double.class).plus((double) m));

        return dsl.select(
                BOOK.BOOK_ID.as("bookId"),
                BOOK.TITLE.as("title"),
                BOOK.COVER_IMAGE_URL.as("coverImageURL")
        )
                .from(BOOK)
                .leftJoin(BOOK_STATS).on(BOOK.BOOK_ID.eq(BOOK_STATS.BOOK_ID))
                .where(BookConditions.discoverableStatus())
                .and(BOOK_STATS.TOTAL_RATING.gt(0L))
                .orderBy(
                        score.desc(),
                        BOOK_STATS.TOTAL_RATING.desc(),
                        BOOK_STATS.AVG_RATING.desc(),
                        BOOK.CREATED_AT.desc(),
                        BOOK.BOOK_ID.desc()
                )
                .limit(limit)
                .fetchInto(BookDTO.class);
    }

    public BookFullDetailDTO getBookFullDetail(int bookId, int accountId) {
        BookFullDetailDTO bookDetail = dsl.select(
                BOOK.TITLE.as("bookName"),
                GENRE.NAME.as("genre"),
                BOOK.COVER_IMAGE_URL.as("coverImageURL"),
                ACCOUNT.DISPLAY_NAME.as("authorName"),
                BOOK.AUTHOR_ACCOUNT_ID.as("authorId"),
                BOOK.STATUS.as("status"),
                BOOK.DESCRIPTION.as("description"),
                BOOK_STATS.TOTAL_VIEW.as("totalView"),
                BOOK_STATS.TOTAL_RATING.as("totalRating"),
                BOOK_STATS.AVG_RATING.as("avgRating"),
                BOOK_STATS.TOTAL_DONATE.as("totalDonate"),
                DSL.when(BOOK.AUTHOR_ACCOUNT_ID.eq(accountId), true).otherwise(false).as("isAuthor"),
                DSL.when(PERSONAL_LIBRARY.BOOK_ID.eq(bookId), true).otherwise(false).as("isInLibrary"),
                DSL.coalesce(RATING.SCORE, DSL.inline(0)).as("userScore"),
                READING_PROGRESS.CURRENT_CHAPTER_ID.as("currentChapterId")
        )
                .from(BOOK)
                .join(GENRE).on(BOOK.GENRE_ID.eq(GENRE.GENRE_ID))
                .join(ACCOUNT).on(BOOK.AUTHOR_ACCOUNT_ID.eq(ACCOUNT.ACCOUNT_ID))
                .leftJoin(BOOK_STATS).on(BOOK.BOOK_ID.eq(BOOK_STATS.BOOK_ID))
                .leftJoin(RATING).on(RATING.BOOK_ID.eq(BOOK.BOOK_ID).and(RATING.ACCOUNT_ID.eq(accountId)))
                .leftJoin(PERSONAL_LIBRARY).on(PERSONAL_LIBRARY.BOOK_ID.eq(BOOK.BOOK_ID)).and(PERSONAL_LIBRARY.ACCOUNT_ID.eq(accountId))
                .leftJoin(READING_PROGRESS).on(READING_PROGRESS.BOOK_ID.eq(BOOK.BOOK_ID)).and(READING_PROGRESS.ACCOUNT_ID.eq(accountId))
                .where(BOOK.BOOK_ID.eq(bookId))
                .and(BOOK.DELETED_AT.isNull())
                .fetchOneInto(BookFullDetailDTO.class);

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

        // Nếu không phải tác giả thì ẩn số donate
        if (!bookDetail.isAuthor()) {
            bookDetail.setTotalDonate(null);
        }

        return bookDetail;
    }

    // Tìm theo tên sách, description
    public PageResponseDTO<BookSummaryDTO> searchBooks(String kw, int page, int size) {
        int offset = (page - 1) * size;

        if (kw == null || kw.trim().isEmpty()) {
            return null;
        }

        Field<String> pattern = DSL.val("%" + kw.trim() + "%");
        var titleCond = BOOK.TITLE.likeIgnoreCase(pattern);
        var descCond = BOOK.DESCRIPTION.likeIgnoreCase(pattern);
        var authorCond = ACCOUNT.DISPLAY_NAME.likeIgnoreCase(pattern);

        // mức độ phù hợp: title +10 description +5 author +7
        Field<Integer> titleScore = DSL.when(titleCond, DSL.inline(10)).otherwise(0);
        Field<Integer> descScore = DSL.when(descCond, DSL.inline(5)).otherwise(0);
        Field<Integer> authorScore = DSL.when(authorCond, DSL.inline(7)).otherwise(0);
        Field<Integer> matchScore = titleScore.add(descScore).add(authorScore);
        Condition searchCondition = titleCond.or(descCond).or(authorCond);
        Condition condition = BookConditions.discoverableStatus().and(ACCOUNT.DELETED_AT.isNull()).and(searchCondition);
        List<BookSummaryDTO> items = dsl.select(
                BOOK.BOOK_ID.as("bookId"),
                BOOK.TITLE.as("title"),
                BOOK.COVER_IMAGE_URL.as("coverImageURL"),
                BOOK_STATS.TOTAL_VIEW.as("totalView"),
                BOOK_STATS.TOTAL_RATING.as("totalRating"),
                BOOK_STATS.AVG_RATING.as("avgRating")
        )
                .from(BOOK)
                .leftJoin(BOOK_STATS).on(BOOK.BOOK_ID.eq(BOOK_STATS.BOOK_ID))
                .join(ACCOUNT).on(ACCOUNT.ACCOUNT_ID.eq(BOOK.AUTHOR_ACCOUNT_ID))
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
                            .join(ACCOUNT).on(ACCOUNT.ACCOUNT_ID.eq(BOOK.AUTHOR_ACCOUNT_ID))
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

        Condition condition = BOOK.GENRE_ID.eq(genreId)
                .and(BookConditions.discoverableStatus());

        SortField<?> orderField;

        var query = dsl.select(
                BOOK.BOOK_ID.as("bookId"),
                BOOK.TITLE.as("title"),
                BOOK.COVER_IMAGE_URL.as("coverImageURL"),
                DSL.coalesce(BOOK_STATS.TOTAL_VIEW, DSL.inline(0)).as("totalView"),
                DSL.coalesce(BOOK_STATS.TOTAL_RATING, DSL.inline(0)).as("totalRating"),
                DSL.coalesce(BOOK_STATS.AVG_RATING, DSL.inline(0.0)).as("avgRating")
        )
                .from(BOOK)
                .leftJoin(BOOK_STATS).on(BOOK.BOOK_ID.eq(BOOK_STATS.BOOK_ID));

        switch (sort != null ? sort.toLowerCase() : "") {
            case "views":
                orderField = BOOK_STATS.TOTAL_VIEW.desc();
                break;

            case "toprated":
                double globalAvg = ratingRepository.getGlobalAvg();
                final int m = 20;
                Field<Double> bayesScore = BOOK_STATS.TOTAL_RATING.cast(Double.class)
                        .mul(BOOK_STATS.AVG_RATING)
                        .plus(DSL.inline((double) m).mul(globalAvg))
                        .divide(BOOK_STATS.TOTAL_RATING.cast(Double.class).plus((double) m));
                orderField = bayesScore.desc();
                break;

            case "newest":
                orderField = BOOK.CREATED_AT.desc();
                break;

            case "trending":
            default:
                orderField = BOOK_STATS.TOTAL_VIEW_7D.desc();
                break;
        }

        List<BookSummaryDTO> items = query
                .where(condition)
                .orderBy(orderField, BOOK.BOOK_ID.desc())
                .limit(size)
                .offset(offset)
                .fetchInto(BookSummaryDTO.class);

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

    public PageResponseDTO<BookSummaryDTO> getBooksByAuthorPaged(int authorId, int page, int size) {
        int offset = (page - 1) * size;

        Condition condition = BookConditions.discoverableStatus().and(BOOK.AUTHOR_ACCOUNT_ID.eq(authorId));

        List<BookSummaryDTO> items = dsl.select(
                BOOK.BOOK_ID.as("bookId"),
                BOOK.TITLE.as("title"),
                BOOK.COVER_IMAGE_URL.as("coverImageURL"),
                BOOK_STATS.TOTAL_VIEW.as("totalView"),
                BOOK_STATS.TOTAL_RATING.as("totalRating"),
                BOOK_STATS.AVG_RATING.as("avgRating")
        )
                .from(BOOK)
                .leftJoin(BOOK_STATS).on(BOOK.BOOK_ID.eq(BOOK_STATS.BOOK_ID))
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

    public PageResponseDTO<BookSummaryAuthorDTO> getMyBooks(int authorId, int page, int size) {
        int offset = (page - 1) * size;

        Condition condition = BOOK.DELETED_AT.isNull().and(BOOK.AUTHOR_ACCOUNT_ID.eq(authorId));

        List<BookSummaryAuthorDTO> items = dsl.select(
                BOOK.BOOK_ID.as("bookId"),
                BOOK.TITLE.as("title"),
                BOOK.COVER_IMAGE_URL.as("coverImageURL"),
                BOOK_STATS.TOTAL_VIEW.as("totalView"),
                BOOK_STATS.TOTAL_RATING.as("totalRating"),
                BOOK_STATS.AVG_RATING.as("avgRating"),
                BOOK_STATS.TOTAL_DONATE.as("totalDonation"),
                BOOK.STATUS.as("status")
        )
                .from(BOOK)
                .leftJoin(BOOK_STATS).on(BOOK.BOOK_ID.eq(BOOK_STATS.BOOK_ID))
                .where(condition)
                .orderBy(BOOK.CREATED_AT.desc(), BOOK.BOOK_ID.desc())
                .limit(size)
                .offset(offset)
                .fetchInto(BookSummaryAuthorDTO.class);

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
    public BookSummaryAuthorDTO createBook(BookInputDTO dto, int authorId) {
        int bookId = dsl.insertInto(BOOK)
                .set(BOOK.TITLE, dto.getTitle())
                .set(BOOK.DESCRIPTION, dto.getDescription())
                .set(BOOK.GENRE_ID, dto.getGenreId())
                .set(BOOK.COVER_IMAGE_URL, dto.getCoverImageURL())
                .set(BOOK.AUTHOR_ACCOUNT_ID, authorId)
                .returning(BOOK.BOOK_ID)
                .fetchOne()
                .getBookId();

        tagRepository.insertTagsIfNotExists(bookId, dto.getTags());

        BookSummaryAuthorDTO newBook = new BookSummaryAuthorDTO(
                bookId,
                dto.getTitle(),
                dto.getCoverImageURL(),
                0, 0, 0, 0,
                "DRAFT"
        );
        return newBook;
    }

    @Transactional
    public boolean updateBook(int bookId, BookInputDTO dto, int authorId) {
        UpdateQuery<?> uq = dsl.updateQuery(BOOK);

        if (dto.getTitle() != null) {
            uq.addValue(BOOK.TITLE, dto.getTitle());
        }
        if (dto.getDescription() != null) {
            uq.addValue(BOOK.DESCRIPTION, dto.getDescription());
        }
        if (dto.getGenreId() != null) {
            uq.addValue(BOOK.GENRE_ID, dto.getGenreId());
        }
        if (dto.getCoverImageURL() != null) {
            uq.addValue(BOOK.COVER_IMAGE_URL, dto.getCoverImageURL());
        }
        if (dto.getStatus() != null) {
            uq.addValue(BOOK.STATUS, dto.getStatus());
        }

        uq.addConditions(
                BOOK.BOOK_ID.eq(bookId)
                        .and(BOOK.DELETED_AT.isNull())
                        .and(BOOK.AUTHOR_ACCOUNT_ID.eq(authorId))
        );

        int updated = uq.execute();
        if (updated == 0 && dto.getTags() == null) {
            throw new IllegalArgumentException(
                    "Tác phẩm không tồn tại, không thuộc về tác giả, hoặc không có trường nào được cập nhật"
            );
        }
        if (dto.getTags() != null) {
            tagRepository.insertTagsIfNotExists(bookId, dto.getTags());
            tagRepository.deleteTagsNotIn(bookId, dto.getTags());
        }

        return true;
    }

    @Transactional
    public void softDeleteBook(int bookId, int authorId) {
        int updated = dsl.update(BOOK)
                .set(BOOK.DELETED_AT, OffsetDateTime.now())
                .where(BOOK.BOOK_ID.eq(bookId))
                .and(BOOK.AUTHOR_ACCOUNT_ID.eq(authorId))
                .execute();

        if (updated == 0) {
            throw new IllegalArgumentException("Book không tồn tại hoặc không thuộc về tác giả");
        }

        dsl.deleteFrom(BOOK_TAG)
                .where(BOOK_TAG.BOOK_ID.eq(bookId))
                .execute();

        dsl.deleteFrom(BOOK_STATS)
                .where(BOOK_STATS.BOOK_ID.eq(bookId))
                .execute();

        dsl.deleteFrom(READING_PROGRESS)
                .where(READING_PROGRESS.BOOK_ID.eq(bookId))
                .execute();

        dsl.deleteFrom(PERSONAL_LIBRARY)
                .where(PERSONAL_LIBRARY.BOOK_ID.eq(bookId))
                .execute();

        // Soft delete tất cả chapter liên quan
        List<Integer> chapterIds = dsl.select(CHAPTER.CHAPTER_ID)
                .from(CHAPTER)
                .where(CHAPTER.BOOK_ID.eq(bookId))
                .fetch(CHAPTER.CHAPTER_ID);

        for (Integer chapterId : chapterIds) {
            chapterRepository.softDeleteChapter(chapterId);
        }
    }
}
