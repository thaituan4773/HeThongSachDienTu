package com.ddtt.services;

import com.ddtt.dtos.EmailVerificationAccountDTO;
import com.ddtt.repositories.AccountRepository;
import com.ddtt.repositories.EmailRepository;
import com.ddtt.utils.JwtUtils;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class EmailService {

    private final EmailRepository emailRepository;
    private final AccountRepository accountRepository;
    private final JwtUtils jwtUtils;

    public boolean sendEmail(String email) {
        var account = accountRepository.findByEmail(email);
        if (account == null){
            throw new SecurityException("Không tìm thấy tài khoản");
        }
        int accountId = account.get("account_id", Integer.class);
        emailRepository.createRequest(email, accountId);
        return true;
    }

    public EmailVerificationAccountDTO findByEmail(String email) {
        return emailRepository.findByEmail(email);
    }
    
    public boolean verifyToken(String token) throws Exception {
        jwtUtils.verifyEmailToken(token);
        return emailRepository.verifyToken(token);
    }
    
}
