package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Value;

@Value
@Serdeable
public class AccountDTO {
    int accountId;
    String displayName;
    String email;
    String avatarURL;
    int balance;
}
