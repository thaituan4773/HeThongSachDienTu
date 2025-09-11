package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import java.util.Date;
import lombok.Value;

@Value
@Serdeable
public class PurchaseHistoryDTO {
    int coin;
    Date date;
}
