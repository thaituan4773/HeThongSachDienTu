package com.ddtt.repositories;

import com.ddtt.dtos.BookDTO;
import com.ddtt.dtos.BookDetailDTO;
import com.ddtt.dtos.ChapterDTO;
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
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

@Singleton
@Blocking
@RequiredArgsConstructor
public class BookRepository {

    private final DSLContext dsl;

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

    public List<BookDTO> findTopRatedBooks(int limit) {
        return dsl.select(
                BOOK.BOOK_ID,
                BOOK.TITLE,
                BOOK.COVER_IMAGE_URL
        )
                .from(BOOK)
                .join(RATING).on(RATING.BOOK_ID.eq(BOOK.BOOK_ID))
                .orderBy(DSL.avg(RATING.SCORE).desc())
                .limit(limit)
                .fetch(record -> new BookDTO(
                record.get(BOOK.BOOK_ID),
                record.get(BOOK.TITLE),
                record.get(BOOK.COVER_IMAGE_URL)
        ));
    }

    public BookDetailDTO findBookDetail(int bookId) {
        // 1. Lấy thông tin sách và thống kê
        BookDetailDTO bookDetail = dsl.select(
                BOOK.TITLE.as("bookName"),
                GENRE.NAME.as("genreName"),
                BOOK.AUTHOR_ACCOUNT_ID.as("authorID"),
                BOOK.DESCRIPTION,
                DSL.count().as("totalView"),
                DSL.count(RATING.BOOK_ID).as("totalRating"),
                DSL.avg(RATING.SCORE).as("avgRating"),
                DSL.coalesce(DSL.sum(DONATION.COIN_AMOUNT), 0).as("totalDonate")
        )
                .from(BOOK)
                .join(GENRE).on(BOOK.GENRE_ID.eq(GENRE.GENRE_ID.cast(Integer.class))) // Cast vẫn cần thiết
                .leftJoin(BOOK_VIEW).on(BOOK_VIEW.BOOK_ID.eq(BOOK.BOOK_ID))
                .leftJoin(RATING).on(RATING.BOOK_ID.eq(BOOK.BOOK_ID))
                .leftJoin(DONATION).on(DONATION.BOOK_ID.eq(BOOK.BOOK_ID))
                .where(BOOK.BOOK_ID.eq(bookId))
                .groupBy(BOOK.BOOK_ID, BOOK.TITLE, GENRE.NAME, BOOK.AUTHOR_ACCOUNT_ID, BOOK.DESCRIPTION)
                .fetchOneInto(BookDetailDTO.class);

        if (bookDetail == null) {
            return null;
        }
        // 2. Truy vấn chapters riêng
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

}
