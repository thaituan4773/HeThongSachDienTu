package com.ddtt.controllers;

import com.ddtt.dtos.RegisterInfoDTO;
import com.ddtt.services.AccountService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Part;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.multipart.CompletedFileUpload;
import lombok.RequiredArgsConstructor;

@Controller("/api/register")
@RequiredArgsConstructor
public class ApiRegisterUser {

    private final AccountService accountService;

    @Post(consumes = "multipart/form-data")
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
            boolean created = accountService.addAccount(dto, avatar);
            if (created) {
                return HttpResponse.created("Account registered successfully");
            } else {
                return HttpResponse.serverError("Failed to create account");
            }
        } catch (IllegalStateException e) {
            return HttpResponse.status(409, "Email already exists");
        } catch (Exception e) {
            return HttpResponse.serverError("Unexpected error: " + e.getMessage());
        }
    }
}
