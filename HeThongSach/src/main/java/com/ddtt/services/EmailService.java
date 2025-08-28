package com.ddtt.services;

import com.ddtt.dtos.EmailVerificationAccountDTO;
import com.ddtt.repositories.EmailRepository;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class EmailService {

    private final EmailRepository emailRepository;

    public boolean sendEmail(String email) {
        return emailRepository.sendVerificationIfAllowed(email);
    }

    public void markEmailVerified(int accountId) {
        emailRepository.markEmailVerified(accountId);
    }

    public EmailVerificationAccountDTO findByEmail(String email) {
        return emailRepository.findByEmail(email);
    }
}
