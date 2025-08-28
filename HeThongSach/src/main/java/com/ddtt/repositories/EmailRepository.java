package com.ddtt.repositories;

import com.ddtt.dtos.EmailVerificationAccountDTO;
import static com.ddtt.jooq.generated.tables.Account.ACCOUNT;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import static com.ddtt.jooq.generated.tables.EmailVerificationRequest.EMAIL_VERIFICATION_REQUEST;
import com.ddtt.utils.JwtUtils;
import io.micronaut.core.annotation.Blocking;
import io.micronaut.email.Email;
import io.micronaut.email.EmailSender;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.jooq.DSLContext;

@RequiredArgsConstructor
@Singleton
@Blocking
public class EmailRepository {

    private final DSLContext dsl;
    private final long cooldownSeconds = 300;
    private final EmailSender<?, ?> emailSender;
    private final String appSchemeBase = "exp+HeThongSach-App://verify?token=";
    private final String webFallbackBase = "http://10.0.2.2:8080/api/verify?token=";
    private final JwtUtils jwtUtils;

    public void sendVerificationEmail(String to, String token) {
        String appLink = appSchemeBase + URLEncoder.encode(token, StandardCharsets.UTF_8);
        String webLink = webFallbackBase + URLEncoder.encode(token, StandardCharsets.UTF_8);
        String body = "Mở app để xác thực email: " + appLink + "\n\nNếu không mở được app, hãy dùng trình duyệt: " + webLink;

        emailSender.send(Email.builder()
                .from("sender@example.com")
                .to(to)
                .subject("Xác thực email")
                .body(body));
    }

    public boolean sendVerificationIfAllowed(String email) {
        var acc = dsl.select(ACCOUNT.ACCOUNT_ID, ACCOUNT.EMAIL_VERIFIED)
                .from(ACCOUNT)
                .where(ACCOUNT.EMAIL.eq(email))
                .fetchOne();

        if (acc == null) {
            throw new IllegalArgumentException("Account not found");
        }
        int accountId = acc.get(ACCOUNT.ACCOUNT_ID);
        Boolean verified = acc.get(ACCOUNT.EMAIL_VERIFIED);
        if (verified != null && verified) {
            return false;
        }

        var rec = dsl.selectFrom(EMAIL_VERIFICATION_REQUEST)
                .where(EMAIL_VERIFICATION_REQUEST.ACCOUNT_ID.eq(accountId))
                .fetchOne();

        OffsetDateTime now = OffsetDateTime.now();
        if (rec != null) {
            OffsetDateTime lastRequest = rec.getRequestedAt();
            if (lastRequest.plusSeconds(cooldownSeconds).isAfter(now)) {
                // chưa đủ 5 phút
                return false;
            }
        }

        String token;
        try {
            token = jwtUtils.generateTokenForEmail(email);
        } catch (Exception e) {
            throw new RuntimeException("Cannot generate token", e);
        }

        if (rec == null) {
            dsl.insertInto(EMAIL_VERIFICATION_REQUEST)
                    .set(EMAIL_VERIFICATION_REQUEST.ACCOUNT_ID, accountId)
                    .set(EMAIL_VERIFICATION_REQUEST.REQUESTED_AT, now)
                    .execute();
        } else {
            dsl.update(EMAIL_VERIFICATION_REQUEST)
                    .set(EMAIL_VERIFICATION_REQUEST.REQUESTED_AT, now)
                    .where(EMAIL_VERIFICATION_REQUEST.ACCOUNT_ID.eq(accountId))
                    .execute();
        }
        sendVerificationEmail(email, token);

        return true;
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

}
