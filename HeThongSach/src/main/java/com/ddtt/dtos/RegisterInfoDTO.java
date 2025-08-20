package com.ddtt.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterInfoDTO {
    @NotBlank(message = "Display name is required")
    @Size(min = 3, max = 20, message = "Display name must be 3..50 chars")
    private String displayName;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    private String email;

    private String avatarURL;
    private String passwordHash;
}
