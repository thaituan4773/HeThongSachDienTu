package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import java.util.Date;
import lombok.Value;

@Value
@Serdeable
public class CoinHistoryDTO {
    String target;
    String description;
    int coin;
    Date date;
}
