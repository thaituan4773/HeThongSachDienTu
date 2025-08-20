package com.ddtt.repositories;

import com.ddtt.auth.EmailVerification;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import static com.ddtt.jooq.generated.tables.EmailVerificationRequest.EMAIL_VERIFICATION_REQUEST;
import static com.ddtt.jooq.generated.tables.Account.ACCOUNT;
import io.micronaut.core.annotation.Blocking;
import io.micronaut.email.Email;
import io.micronaut.email.EmailSender;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.jooq.DSLContext;

@RequiredArgsConstructor
@Singleton
public class EmailRepository {

    private final DSLContext dsl;
    private final EmailVerification tokenService;
    private final long cooldownSeconds = 300;
    private final EmailSender<?, ?> emailSender;
    private final String appSchemeBase = "myapp://verify?token=";
    private final String webFallbackBase = "http://10.0.2.2:8080/api/verify?token=";

    public void sendVerificationEmail(String to, String token) {
        String appLink = appSchemeBase + URLEncoder.encode(token, StandardCharsets.UTF_8);
        String webLink = webFallbackBase + URLEncoder.encode(token, StandardCharsets.UTF_8);
        String body = "Mở app để xác thực email: " + appLink + "\n\nNếu không mở được app, hãy dùng trình duyệt: " + webLink;

        emailSender.send(Email.builder()
                .to(to)
                .subject("Xác thực email")
                .body(body));
    }

    //Các bước thực hiện:
    //1: Kiểm tra account tồn tại & đã verify chưa
    //2: Kiểm tra cooldown trong table email_verification_request
    //3: Sinh token JWT
    //4: Upsert row vào email_verification_request
    //5: Gửi email
    @Blocking
    @Singleton
    public boolean sendVerificationIfAllowed(int accountId, String email) {
        // 1
        var acc = dsl.select(ACCOUNT.ACCOUNT_ID, ACCOUNT.EMAIL_VERIFIED)
                .from(ACCOUNT)
                .where(ACCOUNT.ACCOUNT_ID.eq(accountId))
                .fetchOne();

        if (acc == null) {
            throw new IllegalArgumentException("Account not found");
        }
        Boolean verified = acc.get(ACCOUNT.EMAIL_VERIFIED);
        if (verified != null && verified) {
            return false;
        }

        // 2
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

        // 3
        String token;
        try {
            token = tokenService.generateToken(email);
        } catch (Exception e) {
            throw new RuntimeException("Cannot generate token", e);
        }

        // 4
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
        // 5
        sendVerificationEmail(email, token);

        return true;
    }

}
