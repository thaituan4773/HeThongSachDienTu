package com.ddtt.repositories;

import com.ddtt.dtos.RegisterInfoDTO;
import io.micronaut.core.annotation.Blocking;
import jakarta.inject.Singleton;
import org.jooq.DSLContext;
import static com.ddtt.jooq.generated.tables.Account.ACCOUNT;
import com.ddtt.jooq.generated.tables.records.AccountRecord;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Singleton
@Blocking
@RequiredArgsConstructor
public class AccountRepository {

    private final DSLContext dsl;

    @Transactional
    public int addAccount(RegisterInfoDTO user) {
        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(ACCOUNT)
                        .where(ACCOUNT.EMAIL.eq(user.getEmail()))
                        .and(ACCOUNT.DELETED_AT.isNull())
        );

        if (exists) {
            throw new IllegalStateException("Email already exists");
        }
        var record = dsl.insertInto(ACCOUNT)
                .columns(
                        ACCOUNT.DISPLAY_NAME,
                        ACCOUNT.EMAIL,
                        ACCOUNT.PASSWORD_HASH,
                        ACCOUNT.AVATAR_URL
                )
                .values(
                        user.getDisplayName(),
                        user.getEmail(),
                        user.getPassword(),
                        user.getAvatarURL()
                )
                .returning(ACCOUNT.ACCOUNT_ID)
                .fetchOne();

        if (record == null) {
            throw new RuntimeException("Failed to create account");
        }
        return record.getAccountId();
    }

    public AccountRecord findByEmail(String email) {
        return dsl.selectFrom(ACCOUNT)
                .where(ACCOUNT.EMAIL.eq(email))
                .and(ACCOUNT.DELETED_AT.isNull())
                .fetchOne();
    }

}
