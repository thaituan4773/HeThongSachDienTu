package com.ddtt.services;

import com.ddtt.repositories.DonationRepository;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class DonationService {
    private final DonationRepository donationRepository;
    public boolean createDonation(int donorAccountId, int bookId, int coinAmount){
        return donationRepository.createDonation(donorAccountId, bookId, coinAmount);
    }
}
