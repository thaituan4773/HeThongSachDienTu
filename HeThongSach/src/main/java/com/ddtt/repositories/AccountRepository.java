package com.ddtt.repositories;

import com.ddtt.dtos.AccountDTO;
import com.ddtt.dtos.RegisterInfoDTO;
import io.micronaut.core.annotation.Blocking;
import jakarta.inject.Singleton;
import org.jooq.DSLContext;
import static com.ddtt.jooq.generated.tables.Account.ACCOUNT;
import com.ddtt.jooq.generated.tables.records.AccountRecord;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jooq.Record1;

@Singleton
@Blocking
@RequiredArgsConstructor
public class AccountRepository {

    private final DSLContext dsl;

    @Transactional
    public void addAccount(RegisterInfoDTO user) {
        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(ACCOUNT)
                        .where(ACCOUNT.EMAIL.eq(user.getEmail()))
                        .and(ACCOUNT.DELETED_AT.isNull())
        );

        if (exists) {
            throw new IllegalStateException("Email đã tồn tại");
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
            throw new RuntimeException("Tạo tài khoản thất bại");
        }
    }

    public AccountRecord findByEmail(String email) {
        return dsl.selectFrom(ACCOUNT)
                .where(ACCOUNT.EMAIL.eq(email))
                .and(ACCOUNT.DELETED_AT.isNull())
                .fetchOne();
    }

    public String getRoleById(int id) {
        Record1<String> record = dsl
                .select(ACCOUNT.ROLE)
                .from(ACCOUNT)
                .where(ACCOUNT.ACCOUNT_ID.eq(id))
                .fetchOne();

        if (record == null) {
            throw new SecurityException("Không tìm thấy tài khoản");
        }
        return record.value1();
    }

    public int getIdByEmail(String email) {
        Record1<Integer> record = dsl.select(ACCOUNT.ACCOUNT_ID)
                .from(ACCOUNT)
                .where(ACCOUNT.EMAIL.eq(email))
                .fetchOne();
        if (record == null) {
            throw new SecurityException("Không tìm thấy tài khoản");
        }
        return record.value1();
    }
    
    public AccountDTO getAccountById(int accountId){
        return dsl.select(
                ACCOUNT.ACCOUNT_ID.as("accountId"),
                ACCOUNT.DISPLAY_NAME.as("displayName"),
                ACCOUNT.EMAIL.as("email"),
                ACCOUNT.AVATAR_URL.as("avatarURL"),
                ACCOUNT.BALANCE.as("balance")
        )
                .from(ACCOUNT)
                .where(ACCOUNT.ACCOUNT_ID.eq(accountId))
                .and(ACCOUNT.DELETED_AT.isNull())
                .fetchOneInto(AccountDTO.class);
    }

}
