package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Value;

@Value
@Serdeable
public class AccountInfoDTO {
    int accountId;
    int balance;
}
