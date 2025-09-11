package com.ddtt.repositories;

import com.ddtt.dtos.CoinHistoryDTO;
import com.ddtt.dtos.DonationDTO;
import com.ddtt.dtos.PageResponseDTO;
import io.micronaut.core.annotation.Blocking;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import static com.ddtt.jooq.generated.tables.Donation.DONATION;
import static com.ddtt.jooq.generated.tables.Account.ACCOUNT;
import static com.ddtt.jooq.generated.tables.Book.BOOK;
import static com.ddtt.jooq.generated.tables.Chapter.CHAPTER;
import static com.ddtt.jooq.generated.tables.ChapterPurchase.CHAPTER_PURCHASE;
import jakarta.transaction.Transactional;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

@Singleton
@Blocking
@RequiredArgsConstructor
public class DonationRepository {

    private final DSLContext dsl;

    @Transactional
    public boolean createDonation(int donorAccountId, int bookId, int coinAmount) {

        // Kiểm tra balance
        Integer balance = dsl.select(ACCOUNT.BALANCE)
                .from(ACCOUNT)
                .where(ACCOUNT.ACCOUNT_ID.eq(donorAccountId))
                .forUpdate()
                .fetchOne(ACCOUNT.BALANCE);

        if (balance < coinAmount) {
            return false;
        }

        // Trừ tiền
        dsl.update(ACCOUNT)
                .set(ACCOUNT.BALANCE, ACCOUNT.BALANCE.minus(coinAmount))
                .where(ACCOUNT.ACCOUNT_ID.eq(donorAccountId))
                .execute();

        // Insert donation
        dsl.insertInto(DONATION)
                .set(DONATION.DONOR_ACCOUNT_ID, donorAccountId)
                .set(DONATION.BOOK_ID, bookId)
                .set(DONATION.COIN_AMOUNT, coinAmount)
                .execute();

        return true;
    }

    public List<DonationDTO> findByDonorAccountId(int accountId) {
        return dsl.select(
                BOOK.TITLE.as("bookName"),
                DONATION.COIN_AMOUNT.as("amount"),
                DONATION.CREATED_AT.as("createdAt")
        )
                .from(DONATION)
                .join(BOOK).on(DONATION.BOOK_ID.eq(BOOK.BOOK_ID))
                .where(DONATION.DONOR_ACCOUNT_ID.eq(accountId))
                .orderBy(DONATION.CREATED_AT.desc())
                .fetchInto(DonationDTO.class);
    }

    public PageResponseDTO<CoinHistoryDTO> getCoinSpentHistoryPaged(int accountId, int page, int size) {
        int offset = (page - 1) * size;

        // Donate
        var spendDonate = dsl.select(
                BOOK.TITLE.as("target"),
                DSL.val("Đã ủng hộ").as("description"),
                DONATION.COIN_AMOUNT.as("coin"),
                DONATION.CREATED_AT.as("date")
        )
                .from(DONATION)
                .join(BOOK).on(DONATION.BOOK_ID.eq(BOOK.BOOK_ID))
                .where(DONATION.DONOR_ACCOUNT_ID.eq(accountId));

        // Mua chương
        var spendChapter = dsl.select(
                CHAPTER.TITLE.as("target"),
                DSL.val("Mua chương").as("description"),
                CHAPTER_PURCHASE.COIN_PRICE.as("coin"),
                CHAPTER_PURCHASE.CREATED_AT.as("date")
        )
                .from(CHAPTER_PURCHASE)
                .join(CHAPTER).on(CHAPTER_PURCHASE.CHAPTER_ID.eq(CHAPTER.CHAPTER_ID))
                .where(CHAPTER_PURCHASE.ACCOUNT_ID.eq(accountId));

        // Gộp donate + mua chương
        var combinedTable = spendDonate.unionAll(spendChapter).asTable("combined");

        // Lấy items theo phân trang
        List<CoinHistoryDTO> items = dsl.selectFrom(combinedTable)
                .orderBy(DSL.field("date").desc())
                .limit(size)
                .offset(offset)
                .fetchInto(CoinHistoryDTO.class);

        // Chỉ tính tổng ở page 1
        Long total = null;
        Integer totalPages = null;
        if (page == 1) {
            long cnt = dsl.fetchCount(dsl.selectFrom(combinedTable));
            total = cnt;
            totalPages = (int) Math.ceil((double) cnt / size);
        }

        return new PageResponseDTO<>(total, page, size, totalPages, items);
    }

    public PageResponseDTO<CoinHistoryDTO> getCoinEarnedHistoryPaged(int accountId, int page, int size) {
        int offset = (page - 1) * size;

        // Nhận donate
        var earnDonate = dsl.select(
                BOOK.TITLE.as("target"),
                DSL.concat(DSL.val("Nhận donate từ "), ACCOUNT.DISPLAY_NAME).as("description"),
                DONATION.COIN_AMOUNT.as("coin"),
                DONATION.CREATED_AT.as("date")
        )
                .from(DONATION)
                .join(BOOK).on(DONATION.BOOK_ID.eq(BOOK.BOOK_ID))
                .join(ACCOUNT).on(DONATION.DONOR_ACCOUNT_ID.eq(ACCOUNT.ACCOUNT_ID))
                .where(BOOK.AUTHOR_ACCOUNT_ID.eq(accountId));

        // Nhận xu từ người mua chương
        var earnChapter = dsl.select(
                CHAPTER.TITLE.as("target"),
                DSL.concat(DSL.val("Người mua chương "), BOOK.TITLE).as("description"),
                CHAPTER_PURCHASE.COIN_PRICE.as("coin"),
                CHAPTER_PURCHASE.CREATED_AT.as("date")
        )
                .from(CHAPTER_PURCHASE)
                .join(CHAPTER).on(CHAPTER_PURCHASE.CHAPTER_ID.eq(CHAPTER.CHAPTER_ID))
                .join(BOOK).on(CHAPTER.BOOK_ID.eq(BOOK.BOOK_ID))
                .where(BOOK.AUTHOR_ACCOUNT_ID.eq(accountId));

        // Gộp tất cả
        var combinedTable = earnDonate.unionAll(earnChapter).asTable("combined");

        // Lấy items theo phân trang
        List<CoinHistoryDTO> items = dsl.selectFrom(combinedTable)
                .orderBy(DSL.field("date").desc())
                .limit(size)
                .offset(offset)
                .fetchInto(CoinHistoryDTO.class);

        // Chỉ tính tổng ở page 1
        Long total = null;
        Integer totalPages = null;
        if (page == 1) {
            long cnt = dsl.fetchCount(dsl.selectFrom(combinedTable));
            total = cnt;
            totalPages = (int) Math.ceil((double) cnt / size);
        }

        return new PageResponseDTO<>(total, page, size, totalPages, items);
    }
}
