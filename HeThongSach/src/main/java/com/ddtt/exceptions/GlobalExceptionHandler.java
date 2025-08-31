package com.ddtt.exceptions;

import com.nimbusds.jose.JOSEException;
import io.micronaut.core.cli.exceptions.ParseException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;

import java.util.Map;

@Produces
@Singleton
public class GlobalExceptionHandler implements ExceptionHandler<Exception, HttpResponse<?>> {

    @Override
    public HttpResponse<?> handle(HttpRequest request, Exception ex) {
        if (ex instanceof IllegalStateException) {
            return HttpResponse.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
        }
        if (ex instanceof DuplicateException) {
            return HttpResponse.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", ex.getMessage()));
        }
        if (ex instanceof ParseException) {
            return HttpResponse.badRequest().body(Map.of("error", "Token không phải định dạng JWT hợp lệ"));
        }
        if (ex instanceof JOSEException) {
            return HttpResponse.serverError().body(Map.of("error", "Lỗi thuật toán ký hoặc secret"));
        }
        if (ex instanceof SecurityException) {
            return HttpResponse.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
        }
        if (ex instanceof ForbiddenException) {
            return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
        }
        if (ex instanceof NotFoundException) {
            return HttpResponse.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        }
        if (ex instanceof RuntimeException) {
            return HttpResponse.badRequest().body(Map.of("error", ex.getMessage()));
        }
        return HttpResponse.serverError().body(Map.of("error", "Internal server error: " + ex.getMessage()));
    }
}
