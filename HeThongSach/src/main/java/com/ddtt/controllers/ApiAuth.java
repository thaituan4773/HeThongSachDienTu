package com.ddtt.controllers;

import com.ddtt.dtos.LoginRequestDTO;
import com.ddtt.dtos.RegisterInfoDTO;
import com.ddtt.dtos.TokenResponseDTO;
import com.ddtt.services.AccountService;
import com.ddtt.services.EmailService;
import com.ddtt.services.RefreshTokenService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Part;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.security.authentication.Authentication;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@Controller("/api")
@RequiredArgsConstructor
public class ApiAuth {

    private final AccountService accountService;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;

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

        accountService.addAccount(dto, avatar);
        emailService.sendEmail(email);

        return HttpResponse.created("Account registered successfully");
    }

    @Post("/resend")
    public HttpResponse<Boolean> resend(@Body("email") String email) {
        return HttpResponse.ok(emailService.sendEmail(email));
    }

    @Post("/login")
    public HttpResponse<TokenResponseDTO> login(@Body @Valid LoginRequestDTO request) throws Exception {
        return HttpResponse.ok(accountService.login(request));
    }
    
    @Delete("/logout")
    public HttpResponse logout(Authentication authentication){
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        refreshTokenService.deleteByAccountId(accountId);
        return HttpResponse.noContent();
    }
    
    @Post("/refresh")
    public HttpResponse<?> refreshToken(@Body Map<String, String> request) throws Exception {

        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalStateException("Yêu cầu refresh token");
        }
        return HttpResponse.ok(refreshTokenService.refreshToken(refreshToken));

    }
}
