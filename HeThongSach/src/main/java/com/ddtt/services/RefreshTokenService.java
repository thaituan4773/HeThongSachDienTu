package com.ddtt.services;

import com.ddtt.dtos.TokenResponseDTO;
import com.ddtt.repositories.AccountRepository;
import com.ddtt.repositories.RefreshTokenRepository;
import com.ddtt.utils.JwtUtils;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtils jwtUtils;
    private final AccountRepository accountRepo;

    public boolean isExpired(String refreshToken) {
        return refreshTokenRepository.isValid(refreshToken);
    }

    public String getRefreshTokenByAccountId(int accountId) {
        return refreshTokenRepository.getRefreshTokenByAccountId(accountId);
    }

    public void upsertRefreshToken(int accountId, String token) {
        long duration = jwtUtils.getRefreshExpirationMs();
        OffsetDateTime expiredAt = OffsetDateTime.now().plus(Duration.ofMillis(duration));
        refreshTokenRepository.upsertToken(accountId, token, expiredAt);
    }

    public void deleteByAccountId(int accountId) {
        refreshTokenRepository.deleteByAccountId(accountId);
    }

    public TokenResponseDTO refreshToken(String refreshToken) throws Exception {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Yêu cầu refreshToken");
        }
        
        if (!refreshTokenRepository.isValid(refreshToken)) {
            throw new SecurityException("refreshToken đã hết hạn hoặc không tồn tại");
        }

        String email = jwtUtils.verifyRefreshToken(refreshToken);

        var account = accountRepo.findByEmail(email);
        if (account == null) {
            throw new IllegalStateException("Không tìm thấy tài khoản");
        }

        int accountId = account.get("account_id", Integer.class);
        String newAccessToken = jwtUtils.generateTokenForLogin(email, accountId);
        String newRefreseToken = jwtUtils.generateRefreshToken(email);
        upsertRefreshToken(accountId, newRefreseToken);

        return new TokenResponseDTO(
                newAccessToken,
                newRefreseToken,
                jwtUtils.getLoginExpirationMs(),
                jwtUtils.getRefreshExpirationMs(),
                null
        );
    }
}
