package com.ddtt.services;

import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.dtos.PersonalLibraryBookDTO;
import com.ddtt.repositories.PersonalLibraryRepository;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class PersonalLibraryService {

    private final PersonalLibraryRepository personalLibraryRepository;
    private final int pageSize = 32;

    public PageResponseDTO<PersonalLibraryBookDTO> getPersonalLibrary(
            int accountId,
            String kw,
            int page,
            String sortBy,
            boolean desc,
            Boolean unreaded
    ) {
        return personalLibraryRepository.getPersonalLibrary(accountId, kw, page, pageSize, sortBy, desc, unreaded);
    }

    public void addBookToLibrary(int accountId, int bookId) {
        personalLibraryRepository.addBookToLibrary(accountId, bookId);
    }
    
    public boolean deleteBookFromLibrary(int accountId, int bookId) {
        return personalLibraryRepository.removeBookFromLibrary(accountId, bookId);
    }
}
