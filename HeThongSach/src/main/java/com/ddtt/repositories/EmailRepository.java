package com.ddtt.repositories;

import com.ddtt.dtos.EmailVerificationAccountDTO;
import static com.ddtt.jooq.generated.tables.Account.ACCOUNT;
import static com.ddtt.jooq.generated.tables.EmailChangeRequest.EMAIL_CHANGE_REQUEST;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import static com.ddtt.jooq.generated.tables.EmailVerificationRequest.EMAIL_VERIFICATION_REQUEST;
import com.ddtt.utils.JwtUtils;
import io.micronaut.core.annotation.Blocking;
import io.micronaut.email.Email;
import io.micronaut.email.EmailSender;
import jakarta.transaction.Transactional;
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
    private final String baseLink = "https://randomtoaster.share.zrok.io/";
    private final JwtUtils jwtUtils;

    public void sendVerificationEmail(String to, String token) {
        String appLink = baseLink + "/verify?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        String body = "Mở app để xác thực email: " + appLink;

        emailSender.send(Email.builder()
                .from("sender@example.com")
                .to(to)
                .subject("Xác thực email")
                .body(body));
    }

    public void sendEmailChangeConfirmation(String newEmail, String token) {
        String appLink = baseLink + "/change-email/confirm?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        String body = String.format(
                "Bạn đã yêu cầu đổi email tài khoản. Nhấn vào link bên dưới để xác nhận email mới:\n%s",
                appLink
        );

        emailSender.send(Email.builder()
                .from("sender@example.com")
                .to(newEmail)
                .subject("Xác nhận email mới")
                .body(body));
    }

    public void sendForgotPWRequest(String to, String token) {
        String appLink = baseLink + "/forgot-password/confirm?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        String body = String.format(
                "Bạn vừa yêu cầu đặt lại mật khẩu. Nhấn vào link bên dưới để tiếp tục:\n%s\n\n"
                + "Nếu bạn không yêu cầu, vui lòng bỏ qua email này.",
                appLink
        );

        emailSender.send(Email.builder()
                .from("sender@example.com")
                .to(to)
                .subject("Đặt lại mật khẩu")
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

    @Transactional
    public void createEmailChangeRequest(int accountId, String newEmail, String token, OffsetDateTime expiredAt) {
        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(EMAIL_CHANGE_REQUEST)
                        .where(EMAIL_CHANGE_REQUEST.NEW_EMAIL.eq(newEmail))
                        .and(EMAIL_CHANGE_REQUEST.CONFIRMED.isFalse())
        );
        if (exists) {
            throw new IllegalStateException("Email này đã có yêu cầu thay đổi đang chờ xác nhận");
        }

        dsl.insertInto(EMAIL_CHANGE_REQUEST)
                .columns(
                        EMAIL_CHANGE_REQUEST.ACCOUNT_ID,
                        EMAIL_CHANGE_REQUEST.NEW_EMAIL,
                        EMAIL_CHANGE_REQUEST.TOKEN,
                        EMAIL_CHANGE_REQUEST.EXPIRED_AT
                )
                .values(accountId, newEmail, token, expiredAt)
                .execute();
    }

    @Transactional
    public void confirmEmailChange(String token) {
        var req = dsl.selectFrom(EMAIL_CHANGE_REQUEST)
                .where(EMAIL_CHANGE_REQUEST.TOKEN.eq(token))
                .and(EMAIL_CHANGE_REQUEST.CONFIRMED.isFalse())
                .and(EMAIL_CHANGE_REQUEST.EXPIRED_AT.gt(OffsetDateTime.now()))
                .fetchOne();

        if (req == null) {
            throw new IllegalStateException("Token không hợp lệ hoặc đã hết hạn");
        }

        dsl.update(ACCOUNT)
                .set(ACCOUNT.EMAIL, req.getNewEmail())
                .set(ACCOUNT.EMAIL_VERIFIED, true)
                .where(ACCOUNT.ACCOUNT_ID.eq(req.getAccountId()))
                .execute();

        dsl.update(EMAIL_CHANGE_REQUEST)
                .set(EMAIL_CHANGE_REQUEST.CONFIRMED, true)
                .where(EMAIL_CHANGE_REQUEST.REQUEST_ID.eq(req.getRequestId()))
                .execute();
    }

}
