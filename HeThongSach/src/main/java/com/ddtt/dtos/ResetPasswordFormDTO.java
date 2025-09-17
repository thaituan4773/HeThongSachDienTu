package com.ddtt.dtos;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Value;

@Value
@Introspected
@Serdeable
public class ResetPasswordFormDTO {
    String token;
    String newPassword;
}
