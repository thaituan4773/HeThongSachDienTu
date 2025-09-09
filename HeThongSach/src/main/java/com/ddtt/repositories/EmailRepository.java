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

@Singleton
@Blocking
@RequiredArgsConstructor
public class EmailRepository {

    private final DSLContext dsl;
    private final int expireDuration = 15;
    private final EmailSender<?, ?> emailSender;
    private final String appSchemeBase = "https://randomtoaster.share.zrok.io/api/verify?token=";
    private final JwtUtils jwtUtils;

    public void sendVerificationEmail(String to, String token) {
        String appLink = appSchemeBase + URLEncoder.encode(token, StandardCharsets.UTF_8);
        String body = "Mở app để xác thực email: " + appLink;

        emailSender.send(Email.builder()
                .from("sender@example.com")
                .to(to)
                .subject("Xác thực email")
                .body(body));
    }

    public void createRequest(String email, int accountId) {
        OffsetDateTime now = OffsetDateTime.now();

        String token;
        try {
            token = jwtUtils.generateTokenForEmail(email);
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo token", e);
        }

        OffsetDateTime expiredAt = now.plusMinutes(expireDuration);

        dsl.insertInto(EMAIL_VERIFICATION_REQUEST)
                .set(EMAIL_VERIFICATION_REQUEST.ACCOUNT_ID, accountId)
                .set(EMAIL_VERIFICATION_REQUEST.REQUESTED_AT, now)
                .set(EMAIL_VERIFICATION_REQUEST.TOKEN, token)
                .set(EMAIL_VERIFICATION_REQUEST.EXPIRED_AT, expiredAt)
                .set(EMAIL_VERIFICATION_REQUEST.VERIFIED, false)
                .execute();

        sendVerificationEmail(email, token);
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

    public boolean verifyToken(String token) {
        var request = dsl.selectFrom(EMAIL_VERIFICATION_REQUEST)
                .where(EMAIL_VERIFICATION_REQUEST.TOKEN.eq(token))
                .fetchOne();

        if (request == null) {
            return false; // token không tồn tại
        }

        // Kiểm tra đã verified chưa
        if (request.getVerified() != null && request.getVerified()) {
            return false; // token đã dùng rồi
        }

        // Kiểm tra expired
        OffsetDateTime now = OffsetDateTime.now();
        if (request.getExpiredAt().isBefore(now)) {
            return false; // token đã hết hạn
        }

        int accountId = request.getAccountId();

        // Update trạng thái request
        dsl.update(EMAIL_VERIFICATION_REQUEST)
                .set(EMAIL_VERIFICATION_REQUEST.VERIFIED, true)
                .where(EMAIL_VERIFICATION_REQUEST.REQUEST_ID.eq(request.getRequestId()))
                .execute();

        // Update account
        dsl.update(ACCOUNT)
                .set(ACCOUNT.EMAIL_VERIFIED, true)
                .where(ACCOUNT.ACCOUNT_ID.eq(accountId))
                .execute();

        return true;
    }

}
