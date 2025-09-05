package com.ddtt.services;

import com.ddtt.dtos.CoinPackDTO;
import com.ddtt.repositories.CoinPackRepository;
import jakarta.inject.Singleton;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class CoinPackService {
    private final CoinPackRepository coinPackRepository;
    
    public CoinPackDTO getCoinPackInfo(int coinPackId){
        return coinPackRepository.getCoinPackInfo(coinPackId);
    }
    
    public List<CoinPackDTO> getAllCoinPackInfo(){
        return coinPackRepository.getAllCoinPacks();
    }
}
