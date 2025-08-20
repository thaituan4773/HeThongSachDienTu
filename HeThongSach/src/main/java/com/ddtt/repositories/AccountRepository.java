package com.ddtt.repositories;

import com.ddtt.dtos.EmailVerificationAccountDTO;
import com.ddtt.dtos.RegisterInfoDTO;
import io.micronaut.core.annotation.Blocking;
import jakarta.inject.Singleton;
import org.jooq.DSLContext;
import static com.ddtt.jooq.generated.tables.Account.ACCOUNT;
import jakarta.transaction.Transactional;

@Singleton
@Blocking
public class AccountRepository {

    private final DSLContext dsl;

    public AccountRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public EmailVerificationAccountDTO findByEmail(String email) {
        var record = dsl.selectFrom(ACCOUNT)
                .where(ACCOUNT.EMAIL.eq(email))
                .and(ACCOUNT.DELETED_AT.isNull())
                .fetchOne();
        if (record == null) {
            return null;
        }

        return new EmailVerificationAccountDTO(
                record.getAccountId(),
                record.getEmailVerified()
        );
    }

    public void markEmailVerified(int accountId) {
        dsl.update(ACCOUNT)
                .set(ACCOUNT.EMAIL_VERIFIED, true)
                .where(ACCOUNT.ACCOUNT_ID.eq(accountId))
                .execute();
    }

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
}
