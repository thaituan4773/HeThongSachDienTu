package com.ddtt.controllers;

import com.ddtt.dtos.LoginRequestDTO;
import com.ddtt.dtos.RegisterInfoDTO;
import com.ddtt.services.AccountService;
import com.ddtt.services.EmailService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Part;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@Controller("/api")
@RequiredArgsConstructor
public class ApiAuth {

    private final AccountService accountService;
    private final EmailService emailService;

    @Post(value = "/register", consumes = "multipart/form-data")
    public HttpResponse<?> register(
            @Part("displayName") String displayName,
            @Part("email") String email,
            @Part("password") String password,
            @Nullable @Part(value = "avatar") CompletedFileUpload avatar) {

        RegisterInfoDTO dto = new RegisterInfoDTO();
        dto.setDisplayName(displayName);
        dto.setEmail(email);
        dto.setPassword(password);

        try {
            int accountId = accountService.addAccount(dto, avatar);
            emailService.sendEmail(accountId, email);

            return HttpResponse.created("Account registered successfully");
        } catch (IllegalStateException e) {
            return HttpResponse.status(409, "Email already exists");
        } catch (Exception e) {
            return HttpResponse.serverError("Unexpected error: " + e.getMessage());
        }
    }

    @Post(value = "/login")
    public HttpResponse<?> login(@Body @Valid LoginRequestDTO request) throws Exception {
        return HttpResponse.ok(accountService.login(request));

    }

    @Post(value = "/refresh")
    public HttpResponse<?> refreshToken(@Body Map<String, String> request) throws Exception {

        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalStateException("Refresh token is required");
        }
        return HttpResponse.ok(accountService.refreshToken(refreshToken));

    }
}
