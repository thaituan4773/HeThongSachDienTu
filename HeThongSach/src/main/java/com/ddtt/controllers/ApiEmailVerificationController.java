package com.ddtt.controllers;

import com.ddtt.dtos.EmailVerificationAccountDTO;
import com.ddtt.services.EmailService;
import com.ddtt.utils.JwtUtils;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import lombok.RequiredArgsConstructor;

@Controller("/api/verify")
@RequiredArgsConstructor
public class ApiEmailVerificationController {

    private final EmailService emailService;
    private final JwtUtils jwtUtils;

    @Get
    public HttpResponse<?> verify(@QueryValue String token) throws Exception {
       {
            String email = jwtUtils.verifyEmailToken(token);
            if (email == null) {
                return HttpResponse.badRequest("Invalid or expired token");
            }

            // Tìm lấy thông tin verified email của tài khoản theo email
            EmailVerificationAccountDTO account = emailService.findByEmail(email);
            if (account == null) {
                return HttpResponse.badRequest("Account not found");
            }
            if (account.getEmailVerified()) {
                return HttpResponse.badRequest("Email already verified");
            }
            
            // Cho email verified là true
            emailService.markEmailVerified(account.getAccountId());
            return HttpResponse.ok("Email verified successfully");
        }
    }
}
