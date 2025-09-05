package com.ddtt.repositories;

import io.micronaut.core.annotation.Blocking;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import static com.ddtt.jooq.generated.tables.Donation.DONATION;
import static com.ddtt.jooq.generated.tables.Account.ACCOUNT;
import jakarta.transaction.Transactional;
import org.jooq.DSLContext;

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
}
