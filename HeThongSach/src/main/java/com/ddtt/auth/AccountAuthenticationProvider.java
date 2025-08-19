package com.ddtt.auth;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationFailureReason;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.provider.HttpRequestAuthenticationProvider;
import io.micronaut.core.annotation.Blocking;
import jakarta.inject.Singleton;
import org.jooq.DSLContext;
import org.mindrot.jbcrypt.BCrypt;
import static com.ddtt.jooq.generated.tables.Account.ACCOUNT;

import java.util.List;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class AccountAuthenticationProvider implements HttpRequestAuthenticationProvider<Object> {

    private final DSLContext dsl;

    @Blocking
    @Override
    public AuthenticationResponse authenticate(@Nullable HttpRequest<Object> httpRequest,
            @NonNull AuthenticationRequest<String, String> authRequest) {
        String email = authRequest.getIdentity();
        String password = authRequest.getSecret();

        var record = dsl.selectFrom(ACCOUNT)
                .where(ACCOUNT.EMAIL.eq(email))
                .and(ACCOUNT.DELETED_AT.isNull())
                .fetchOne();

        if (record != null && BCrypt.checkpw(password, record.getPasswordHash())) {
            if (!record.getEmailVerified()) {
                return AuthenticationResponse.failure("EMAIL_NOT_VERIFIED");
            }
            return AuthenticationResponse.success(
                    record.getEmail(),
                    List.of(record.getRole())
            );
        }

        return AuthenticationResponse.failure(AuthenticationFailureReason.CREDENTIALS_DO_NOT_MATCH);
    }
}
