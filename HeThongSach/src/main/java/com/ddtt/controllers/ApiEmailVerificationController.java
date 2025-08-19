package com.ddtt.controllers;

import com.ddtt.auth.EmailVerificationService;
import com.ddtt.dtos.EmailVerificationAccountDTO;
import com.ddtt.services.AccountService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import jakarta.inject.Inject;
import org.jooq.DSLContext;

@Controller("/api/verify-email")
public class ApiEmailVerificationController {

    @Inject
    EmailVerificationService emailService;
    @Inject
    AccountService accountService;

    public ApiEmailVerificationController(DSLContext dsl) {
    }

    @Get
    public HttpResponse<?> verify(@QueryValue String token) {
        try {
            String email = emailService.verifyToken(token);
            if (email == null) {
                return HttpResponse.badRequest("Invalid or expired token");
            }

            // Tìm tài khoản theo email
            EmailVerificationAccountDTO account = accountService.findByEmail(email);
            if (account == null) {
                return HttpResponse.badRequest("Account not found");
            }
            if (account.getEmailVerified()) {
                return HttpResponse.badRequest("Email already verified");
            }
            
            // Cho email verified là true
            accountService.markEmailVerified(account.getAccountId());
            return HttpResponse.ok("Email verified successfully");
        } catch (Exception e) {
            return HttpResponse.badRequest("Invalid token");
        }
    }
}
