package com.ddtt.dtos;

import lombok.Value;

@Value
public class EmailVerificationAccountDTO {
    int accountId;
    Boolean emailVerified;
}
