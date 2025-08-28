package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor
@Serdeable
public class TokenResponseDTO {
    // nếu email chưa xác thực, chỉ trả email, còn lại null
    String accessToken;
    String refreshToken;
    long expiresAt;
    long refreshExpiresAt;
    String email;
}
