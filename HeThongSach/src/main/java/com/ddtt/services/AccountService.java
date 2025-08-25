package com.ddtt.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ddtt.dtos.LoginRequestDTO;
import com.ddtt.dtos.TokenResponseDTO;
import com.ddtt.dtos.RegisterInfoDTO;
import com.ddtt.exceptions.ForbiddenException;
import com.ddtt.jooq.generated.tables.records.AccountRecord;
import com.ddtt.repositories.AccountRepository;
import com.ddtt.utils.JwtUtils;
import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;

@Singleton
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepo;
    private final Cloudinary cloudinary;
    private final JwtUtils jwtUtils;

    public int addAccount(RegisterInfoDTO info, CompletedFileUpload file) {
        if (file != null && file.getSize() > 0) {
            try {
                Map res = cloudinary.uploader()
                        .upload(file.getBytes(), ObjectUtils.asMap("resource_type", "auto"));
                info.setAvatarURL(res.get("secure_url").toString());
            } catch (IOException ex) {
                throw new RuntimeException("Lỗi upload ảnh lên Cloudinary", ex);
            }
        } else { // Avatar mặt định nếu không có avatar
            info.setAvatarURL("https://res.cloudinary.com/dddfgg9yo/image/upload/v1755609196/bl1rwq6xq5biwgawjxwn.webp");
        }
        String hashed = BCrypt.hashpw(info.getPassword(), BCrypt.gensalt());
        info.setPassword(hashed);
        return accountRepo.addAccount(info);
    }

    public TokenResponseDTO login(LoginRequestDTO req) throws Exception {
        AccountRecord account = accountRepo.findByEmail(req.getEmail());
        if (account == null) {
            throw new SecurityException("Email hoặc password sai");
        }
        String hash = account.get("password_hash", String.class);
        if (!BCrypt.checkpw(req.getPassword(), hash)) {
            throw new SecurityException("Email hoặc password sai");
        }
        if (!account.get("email_verified", Boolean.class)) {
            throw new ForbiddenException("Email chưa xác thực");
        }

        int accountId = accountRepo.getIdByEmail(account.get("email", String.class));
        String accessToken = jwtUtils.generateTokenForLogin(account.get("email", String.class), accountId);
        String refreshToken = jwtUtils.generateRefreshToken(account.get("email", String.class));
        return new TokenResponseDTO(
                accessToken,
                refreshToken,
                jwtUtils.getLoginExpirationMs(),
                jwtUtils.getRefreshExpirationMs()
        );
    }

    public TokenResponseDTO refreshToken(String refreshToken) throws Exception {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }

        String email = jwtUtils.verifyRefreshToken(refreshToken);

        var account = accountRepo.findByEmail(email);
        if (account == null) {
            throw new RuntimeException("Account not found");
        }
        int accountId = account.get("account_id", Integer.class);
        String newAccessToken = jwtUtils.generateTokenForLogin(email, accountId);
        String newRefreshToken = jwtUtils.generateRefreshToken(email);

        return new TokenResponseDTO(
                newAccessToken,
                newRefreshToken,
                jwtUtils.getLoginExpirationMs(),
                jwtUtils.getRefreshExpirationMs()
        );
    }

    public String getRoleById(int id) {
        return accountRepo.getRoleById(id);
    }

    public int getIdByEmail(String email) {
        return accountRepo.getIdByEmail(email);
    }

}
