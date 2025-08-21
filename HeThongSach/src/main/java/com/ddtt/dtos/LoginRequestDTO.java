package com.ddtt.dtos;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
@Introspected
@Serdeable.Deserializable
public class LoginRequestDTO {
    @NotBlank
    private String email;
    @NotBlank
    private String password;
}
