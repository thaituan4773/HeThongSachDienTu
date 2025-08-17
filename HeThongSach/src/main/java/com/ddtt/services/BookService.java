package com.ddtt.services;

import com.ddtt.dtos.BookDTO;
import com.ddtt.jooq.generated.tables.Book;
import com.ddtt.jooq.generated.tables.records.BookRecord;
import com.ddtt.repositories.BookRepository;
import jakarta.inject.Singleton;
import java.util.List;

@Singleton
public class BookService {

    private final BookRepository bookRepo;

    public BookService(BookRepository bookRepo) {
        this.bookRepo = bookRepo;
    }
    
    public List<BookDTO> getAllBooks(){
        return bookRepo.findAllBooks();
    }
}
