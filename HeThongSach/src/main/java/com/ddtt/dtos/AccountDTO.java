package com.ddtt.dtos;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import lombok.Data;

@Data
@Serdeable
public class AccountDTO {
    int accountId;
    String displayName;
    String bio;
    String avatarURL;
    int balance;
    List<BookDTO> books; // chỉ fetch một vài
    int booksTotal;
}
