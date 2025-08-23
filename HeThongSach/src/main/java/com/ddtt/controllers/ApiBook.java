package com.ddtt.controllers;

import com.ddtt.dtos.BookDetailDTO;
import com.ddtt.services.BookService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Get;
import lombok.RequiredArgsConstructor;

@Controller("/api")
@RequiredArgsConstructor
public class ApiBook {

    private final BookService bookService;

    @Get("/books/{bookId}")
    public HttpResponse<BookDetailDTO> viewBookDetail(@PathVariable int bookId) {
        BookDetailDTO bookDetail = bookService.findBookDetail(bookId);
        if (bookDetail == null) {
            return HttpResponse.notFound();
        }
        return HttpResponse.ok(bookDetail);
    }
    
    
}
