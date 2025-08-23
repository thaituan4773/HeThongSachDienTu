package com.ddtt.controllers;

import com.ddtt.dtos.BookDTO;
import com.ddtt.services.BookService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.authentication.Authentication;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@Controller("/api")
@RequiredArgsConstructor
public class ApiController {
    private final BookService bookService;
    
    @Get("/books")
    public HttpResponse<List<BookDTO>> getAllBooks() {
        return HttpResponse.ok(bookService.getAllBooks());
    }
    
    @Get("/me")
    public Object me(@Nullable Authentication authentication) {
        if (authentication == null) {
            return Map.of("authenticated", false);
        }
        return Map.of(
            "authenticated", true,
            "name", authentication.getName(),
            "roles", authentication.getRoles(),     // list roles
            "attributes", authentication.getAttributes()
        );
    }
}
