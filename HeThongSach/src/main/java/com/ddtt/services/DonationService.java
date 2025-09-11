package com.ddtt.services;

import com.ddtt.dtos.CoinHistoryDTO;
import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.repositories.DonationRepository;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class DonationService {

    private final DonationRepository donationRepository;
    private final int size = 50;

    public boolean createDonation(int donorAccountId, int bookId, int coinAmount) {
        return donationRepository.createDonation(donorAccountId, bookId, coinAmount);
    }

    public PageResponseDTO<CoinHistoryDTO> getCoinSpentHistory(int accountId, int page) {
        return donationRepository.getCoinSpentHistoryPaged(accountId, page, size);
    }

    public PageResponseDTO<CoinHistoryDTO> getCoinEarnedHistory(int accountId, int page) {
        return donationRepository.getCoinEarnedHistoryPaged(accountId, page, size);
    }
}
