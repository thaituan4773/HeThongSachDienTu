package com.ddtt.dtos;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Value;

@Value
@Serdeable
@Introspected
public class CoinPackDTO {
    int coinPackId;
    String coinPackName;
    int coinAmount;
    long price;
}
