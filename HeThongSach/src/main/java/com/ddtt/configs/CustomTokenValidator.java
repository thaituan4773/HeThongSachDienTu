package com.ddtt.configs;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.order.Ordered;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.token.validator.TokenValidator;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import java.util.List;
import java.util.Map;

@Singleton
public class CustomTokenValidator implements TokenValidator<HttpRequest<?>> {

    private final com.ddtt.utils.JwtUtils jwtUtils;
    private final com.ddtt.services.AccountService accountService;

    public CustomTokenValidator(com.ddtt.utils.JwtUtils jwtUtils,
            com.ddtt.services.AccountService accountService) {
        this.jwtUtils = jwtUtils;
        this.accountService = accountService;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @NonNull
    @Override
    public Publisher<Authentication> validateToken(@NonNull String token, HttpRequest<?> request) {
        try {
            if (token.isBlank()) {
                return Publishers.empty();
            }

            String email = jwtUtils.verifyLoginToken(token);
            if (email == null || email.isBlank()) {
                return Publishers.empty();
            }

            String role = accountService.getRoleByEmail(email);
            List<String> rolesList;
            if (role == null || role.isBlank()) {
                rolesList = List.of();
            } else {
                rolesList = List.of(role.toUpperCase());
            }

            Map<String, Object> attributes = Map.of("email", email);


            Authentication auth = Authentication.build(email, rolesList, attributes);

            return Publishers.just(auth);
        } catch (Exception ex) {
            return Publishers.empty();
        }
    }
}
