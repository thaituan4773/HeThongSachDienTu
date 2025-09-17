package com.ddtt.repositories;

import com.ddtt.dtos.AccountDTO;
import com.ddtt.dtos.AccountPatchDTO;
import com.ddtt.dtos.BookDTO;
import com.ddtt.dtos.RegisterInfoDTO;
import com.ddtt.exceptions.NotFoundException;
import io.micronaut.core.annotation.Blocking;
import jakarta.inject.Singleton;
import org.jooq.DSLContext;
import static com.ddtt.jooq.generated.tables.Account.ACCOUNT;
import static com.ddtt.jooq.generated.tables.Book.BOOK;
import static com.ddtt.jooq.generated.tables.PasswordResetRequest.PASSWORD_RESET_REQUEST;
import com.ddtt.jooq.generated.tables.records.AccountRecord;
import com.ddtt.repository.conditions.BookConditions;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.Record1;
import org.jooq.UpdateQuery;
import org.jooq.impl.DSL;
import org.mindrot.jbcrypt.BCrypt;

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

    public String getPasswordHash(int accountId) {
        return dsl.select(ACCOUNT.PASSWORD_HASH)
                .from(ACCOUNT)
                .where(ACCOUNT.ACCOUNT_ID.eq(accountId))
                .and(ACCOUNT.DELETED_AT.isNull())
                .fetchOneInto(String.class);

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

    public AccountDTO getProfile(int accountId, int limit) {
        AccountDTO acc = dsl.select(
                ACCOUNT.ACCOUNT_ID.as("accountId"),
                ACCOUNT.DISPLAY_NAME.as("displayName"),
                ACCOUNT.BIO.as("bio"),
                ACCOUNT.AVATAR_URL.as("avatarURL"),
                ACCOUNT.BALANCE.as("balance"),
                DSL.field(
                        DSL.selectCount()
                                .from(BOOK)
                                .where(BOOK.AUTHOR_ACCOUNT_ID.eq(ACCOUNT.ACCOUNT_ID))
                                .and(BookConditions.discoverableStatus())
                ).as("booksTotal")
        )
                .from(ACCOUNT)
                .where(ACCOUNT.ACCOUNT_ID.eq(accountId))
                .and(ACCOUNT.DELETED_AT.isNull())
                .fetchOneInto(AccountDTO.class);

        if (acc == null) {
            throw new IllegalStateException("Không tìm thấy tài khoản");
        }

        List<BookDTO> books = dsl.select(
                BOOK.BOOK_ID.as("bookId"),
                BOOK.TITLE,
                BOOK.COVER_IMAGE_URL.as("coverImageURL")
        )
                .from(BOOK)
                .where(BOOK.AUTHOR_ACCOUNT_ID.eq(accountId))
                .and(BookConditions.discoverableStatus())
                .orderBy(BOOK.CREATED_AT.desc())
                .limit(limit)
                .fetchInto(BookDTO.class);

        acc.setBooks(books);
        return acc;
    }

    public int getBalance(int accountId) {
        return dsl.select(ACCOUNT.BALANCE)
                .from(ACCOUNT)
                .where(ACCOUNT.ACCOUNT_ID.eq(accountId))
                .fetchOneInto(Integer.class);
    }

    @Transactional
    public void createForgotPWRequest(int accountId, String token, OffsetDateTime expiredAt) {
        dsl.insertInto(PASSWORD_RESET_REQUEST)
                .columns(
                        PASSWORD_RESET_REQUEST.ACCOUNT_ID,
                        PASSWORD_RESET_REQUEST.TOKEN,
                        PASSWORD_RESET_REQUEST.EXPIRED_AT
                )
                .values(accountId, token, expiredAt)
                .execute();
    }

    @Transactional
    public void confirmForgotPWRequest(String token, String newHashedPassword) {
        var req = dsl.selectFrom(PASSWORD_RESET_REQUEST)
                .where(PASSWORD_RESET_REQUEST.TOKEN.eq(token))
                .and(PASSWORD_RESET_REQUEST.USED.isFalse())
                .and(PASSWORD_RESET_REQUEST.EXPIRED_AT.gt(OffsetDateTime.now()))
                .fetchOne();

        if (req == null) {
            throw new IllegalStateException("Token không hợp lệ hoặc đã hết hạn");
        }

        dsl.update(ACCOUNT)
                .set(ACCOUNT.PASSWORD_HASH, newHashedPassword)
                .where(ACCOUNT.ACCOUNT_ID.eq(req.getAccountId()))
                .execute();

        dsl.update(PASSWORD_RESET_REQUEST)
                .set(PASSWORD_RESET_REQUEST.USED, true)
                .where(PASSWORD_RESET_REQUEST.REQUEST_ID.eq(req.getRequestId()))
                .execute();
    }

    public void changePassword(int accountId, String newHashedPassword) {
        dsl.update(ACCOUNT)
                .set(ACCOUNT.PASSWORD_HASH, newHashedPassword)
                .where(ACCOUNT.ACCOUNT_ID.eq(accountId))
                .execute();
    }

    public void patchAccount(int accountId, AccountPatchDTO dto) {
        // Kiểm tra account còn tồn tại
        boolean accountExists = dsl.fetchExists(
                DSL.selectOne()
                        .from(ACCOUNT)
                        .where(ACCOUNT.ACCOUNT_ID.eq(accountId))
                        .and(ACCOUNT.DELETED_AT.isNull())
        );

        if (!accountExists) {
            throw new NotFoundException("Tài khoản không tồn tại hoặc đã bị xóa");
        }

        // Tạo câu lệnh update động
        UpdateQuery<?> uq = dsl.updateQuery(ACCOUNT);

        if (dto.getDisplayName() != null) {
            uq.addValue(ACCOUNT.DISPLAY_NAME, dto.getDisplayName());
        }
        if (dto.getAvatarURL() != null) {
            uq.addValue(ACCOUNT.AVATAR_URL, dto.getAvatarURL());
        }
        if (dto.getBio() != null) {
            uq.addValue(ACCOUNT.BIO, dto.getBio());
        }

        uq.addConditions(ACCOUNT.ACCOUNT_ID.eq(accountId));

        int updated = uq.execute();
        if (updated == 0) {
            throw new IllegalArgumentException("Không có trường nào được cập nhật");
        }
    }

}
