package com.ddtt.dtos;

import lombok.Value;

@Value
public class CoinPackDTO {
    int coinPackId;
    String coinPackName;
    int coinAmount;
    long price;
}
