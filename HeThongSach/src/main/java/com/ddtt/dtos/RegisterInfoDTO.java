package com.ddtt.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterInfoDTO {
    @NotBlank(message = "Yêu cầu tên hiển thị")
    @Size(min = 3, max = 20, message = "Tên hiện thị phải dài từ 3 đến 20 ký tự")
    String displayName;

    @NotBlank(message = "Yêu cầu mật khẩu")
    @Size(min = 6, message = "Mật khẩu phải ít nhất 6 ký tự")
    String password;

    @NotBlank(message = "Yêu cầu email")
    @Email(message = "Email không hợp lệ")
    String email;

    String avatarURL;
    String passwordHash;
}
