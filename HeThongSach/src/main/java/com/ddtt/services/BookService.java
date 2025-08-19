package com.ddtt.services;

import com.ddtt.dtos.BookDTO;
import com.ddtt.repositories.BookRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;

@Singleton
public class BookService {

    @Inject
    private BookRepository bookRepo;

    
    public List<BookDTO> getAllBooks(){
        return bookRepo.findAllBooks();
    }
}
