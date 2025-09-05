package com.ddtt.repositories;

import com.ddtt.dtos.CoinPackDTO;
import io.micronaut.core.annotation.Blocking;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import static com.ddtt.jooq.generated.tables.CoinPack.COIN_PACK;
import java.util.List;

@Singleton
@Blocking
@RequiredArgsConstructor
public class CoinPackRepository {

    private final DSLContext dsl;

    public CoinPackDTO getCoinPackInfo(int coinPackId) {
        return dsl.select(
                COIN_PACK.COIN_PACK_ID.as("coinPackId"),
                COIN_PACK.NAME.as("coinPackName"),
                COIN_PACK.COIN_AMOUNT.as("coinAmount"),
                COIN_PACK.PRICE.as("price")
        )
                .from(COIN_PACK)
                .where(COIN_PACK.COIN_PACK_ID.eq(coinPackId))
                .fetchOneInto(CoinPackDTO.class);
    }

    public List<CoinPackDTO> getAllCoinPacks() {
        return dsl.select(
                COIN_PACK.COIN_PACK_ID.as("coinPackId"),
                COIN_PACK.NAME.as("coinPackName"),
                COIN_PACK.COIN_AMOUNT.as("coinAmount"),
                COIN_PACK.PRICE.as("price")
        )
                .from(COIN_PACK)
                .where(COIN_PACK.ACTIVE.eq(Boolean.TRUE))
                .fetchInto(CoinPackDTO.class);
    }
}
