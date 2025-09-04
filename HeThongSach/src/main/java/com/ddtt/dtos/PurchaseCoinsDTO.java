package com.ddtt.dtos;

import java.time.OffsetDateTime;
import lombok.Value;

@Value
public class PurchaseCoinsDTO {
    int transactionId;
    int accountId;
    int coinAmount;
    long moneyAmount;
    String paymentMethod;
    String status;
    OffsetDateTime createdAt;
}
