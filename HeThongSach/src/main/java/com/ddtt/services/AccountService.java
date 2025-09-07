package com.ddtt.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ddtt.dtos.AccountDTO;
import com.ddtt.dtos.LoginRequestDTO;
import com.ddtt.dtos.TokenResponseDTO;
import com.ddtt.dtos.RegisterInfoDTO;
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
    private final RefreshTokenService refreshTokenService;

    public void addAccount(RegisterInfoDTO info, CompletedFileUpload file) {
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
        accountRepo.addAccount(info);
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
        String email = account.get("email", String.class);
        if (!account.get("email_verified", Boolean.class)) {
            return new TokenResponseDTO(
                    null,
                    null,
                    0L,
                    0L,
                    email
            );
        }
        int accountId = accountRepo.getIdByEmail(email);
        
        String refreshToken = refreshTokenService.getRefreshTokenByAccountId(accountId);
        
        if(refreshToken == null){
            refreshToken = jwtUtils.generateRefreshToken(email);
            refreshTokenService.upsertRefreshToken(accountId, refreshToken);
        }
            
        String accessToken = jwtUtils.generateTokenForLogin(email, accountId);
        return new TokenResponseDTO(
                accessToken,
                refreshToken,
                jwtUtils.getLoginExpirationMs(),
                jwtUtils.getRefreshExpirationMs(),
                null
        );
    }

    public String getRoleById(int id) {
        return accountRepo.getRoleById(id);
    }

    public int getIdByEmail(String email) {
        return accountRepo.getIdByEmail(email);
    }

    public AccountDTO getAccountById(int accountId) {
        int limit = 6;
        return accountRepo.getProfile(accountId, limit);
    }

    public int getBalance(int accountId) {
        return accountRepo.getBalance(accountId);
    }
    
    public AccountRecord findByEmail(String email){
        return accountRepo.findByEmail(email);
    } 

}
