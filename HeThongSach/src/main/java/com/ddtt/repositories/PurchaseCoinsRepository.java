package com.ddtt.repositories;

import com.ddtt.dtos.PurchaseCoinsDTO;
import io.micronaut.core.annotation.Blocking;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import static com.ddtt.jooq.generated.tables.PurchaseCoins.PURCHASE_COINS;
import static com.ddtt.jooq.generated.tables.CoinPack.COIN_PACK;
import java.util.UUID;

@Singleton
@Blocking
@RequiredArgsConstructor
public class PurchaseCoinsRepository {

    private final DSLContext dsl;

    public UUID createPurchase(int accountId, int coinPackId, String paymentMethod) {
        var record = dsl.insertInto(PURCHASE_COINS)
                .set(PURCHASE_COINS.ACCOUNT_ID, accountId)
                .set(PURCHASE_COINS.COIN_PACK_ID, coinPackId)
                .set(PURCHASE_COINS.PAYMENT_METHOD, paymentMethod)
                .set(PURCHASE_COINS.STATUS, "PENDING")
                .returning(PURCHASE_COINS.TRANSACTION_ID)
                .fetchOne();

        if (record == null) {
            throw new IllegalStateException("Tạo đơn hàng thất bại");
        }
        return record.getTransactionId();
    }

    public PurchaseCoinsDTO findByTransactionId(UUID transactionId) {
        PurchaseCoinsDTO result = dsl.select(
                PURCHASE_COINS.TRANSACTION_ID.as("transactionId"),
                PURCHASE_COINS.ACCOUNT_ID.as("accountId"),
                COIN_PACK.COIN_AMOUNT.as("coinAmount"),
                COIN_PACK.PRICE.as("moneyAmount"),
                PURCHASE_COINS.PAYMENT_METHOD.as("paymentMethod"),
                PURCHASE_COINS.STATUS.as("status"),
                PURCHASE_COINS.CREATED_AT.as("createdAt")
        )
                .from(PURCHASE_COINS)
                .join(COIN_PACK).on(PURCHASE_COINS.COIN_PACK_ID.eq(COIN_PACK.COIN_PACK_ID))
                .where(PURCHASE_COINS.TRANSACTION_ID.eq(transactionId))
                .fetchOneInto(PurchaseCoinsDTO.class);
        return result;
    }

    public int markPaid(UUID transactionId) {
        return dsl.update(PURCHASE_COINS)
                .set(PURCHASE_COINS.STATUS, "PAID")
                .set(PURCHASE_COINS.PAYMENT_METHOD, "MOMO")
                .where(PURCHASE_COINS.TRANSACTION_ID.eq(transactionId))
                .and(PURCHASE_COINS.STATUS.eq("PENDING"))
                .execute();
    }

    public void updateStatus(UUID orderId, String status) {
        int updated = dsl.update(PURCHASE_COINS)
                .set(PURCHASE_COINS.STATUS, status)
                .where(PURCHASE_COINS.TRANSACTION_ID.eq(orderId))
                .execute();

        if (updated == 0) {
            throw new IllegalStateException("Transaction not found or cannot update: " + orderId);
        }
    }
    
    
}
