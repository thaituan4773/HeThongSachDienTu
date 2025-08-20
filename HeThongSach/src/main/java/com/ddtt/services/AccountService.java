package com.ddtt.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ddtt.dtos.EmailVerificationAccountDTO;
import com.ddtt.dtos.RegisterInfoDTO;
import com.ddtt.repositories.AccountRepository;
import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;

@RequiredArgsConstructor
@Singleton
public class AccountService {

    private final AccountRepository accountRepo;
    private final Cloudinary cloudinary;
    
    public EmailVerificationAccountDTO findByEmail(String email) {
        return accountRepo.findByEmail(email);
    }
    
    public void markEmailVerified(int accountId) {
        accountRepo.markEmailVerified(accountId);
    }
    
    public int addAccount(RegisterInfoDTO info, CompletedFileUpload file){
        if (file != null && file.getSize() > 0) {
            try {
                Map res = cloudinary.uploader()
                        .upload(file.getBytes(), ObjectUtils.asMap("resource_type", "auto"));
                info.setAvatarURL(res.get("secure_url").toString());
            } catch (IOException ex) {
                throw new RuntimeException("Lỗi upload ảnh lên Cloudinary", ex);
            }
        }
        else{ // Avatar mặt định nếu không có avatar
            info.setAvatarURL("https://res.cloudinary.com/dddfgg9yo/image/upload/v1755609196/bl1rwq6xq5biwgawjxwn.webp");
        }
         String hashed = BCrypt.hashpw(info.getPassword(), BCrypt.gensalt());
        info.setPassword(hashed);
        return this.accountRepo.addAccount(info);
    }
}
