package com.ddtt.repositories;

import io.micronaut.core.annotation.Blocking;
import jakarta.inject.Singleton;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import static com.ddtt.jooq.generated.tables.RefreshToken.REFRESH_TOKEN;
import org.jooq.DSLContext;

@Singleton
@Blocking
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private final DSLContext dsl;

    public void upsertToken(int accountId, String token, OffsetDateTime expiredAt) {
        dsl.insertInto(REFRESH_TOKEN)
                .set(REFRESH_TOKEN.ACCOUNT_ID, accountId)
                .set(REFRESH_TOKEN.TOKEN, token)
                .set(REFRESH_TOKEN.EXPIRED_AT, expiredAt)
                .onConflict(REFRESH_TOKEN.ACCOUNT_ID)
                .doUpdate()
                .set(REFRESH_TOKEN.TOKEN, token)
                .set(REFRESH_TOKEN.EXPIRED_AT, expiredAt)
                .execute();
    }

    public boolean isValid(String token) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(REFRESH_TOKEN)
                        .where(REFRESH_TOKEN.TOKEN.eq(token))
                        .and(REFRESH_TOKEN.EXPIRED_AT.gt(OffsetDateTime.now()))
        );
    }

    public String getRefreshTokenByAccountId(int accountId) {
        var result = dsl.select(REFRESH_TOKEN.TOKEN)
                .from(REFRESH_TOKEN)
                .where(REFRESH_TOKEN.ACCOUNT_ID.eq(accountId))
                .and(REFRESH_TOKEN.EXPIRED_AT.gt(OffsetDateTime.now()))
                .fetchOneInto(String.class);
        return result;
    }

    public void deleteByAccountId(int accountId) {
        dsl.deleteFrom(REFRESH_TOKEN)
                .where(REFRESH_TOKEN.ACCOUNT_ID.eq(accountId))
                .execute();
    }

}
