package com.ddtt.controllers;

import com.ddtt.dtos.BookDTO;
import com.ddtt.services.BookService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import java.util.List;

@Controller("/api")
public class ApiController {
    private final BookService bookService;

    public ApiController(BookService bookService) {
        this.bookService = bookService;
    }
    
    @Get("/books")
    public HttpResponse<List<BookDTO>> getAllBooks() {
        return HttpResponse.ok(bookService.getAllBooks());
    }
}
