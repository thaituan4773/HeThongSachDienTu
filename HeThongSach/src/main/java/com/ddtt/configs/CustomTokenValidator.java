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
            int accountId = jwtUtils.verifyLoginToken(token);
            String role = accountService.getRoleById(accountId);
            List<String> rolesList = (role == null || role.isBlank())
                    ? List.of()
                    : List.of(role.toUpperCase());

            Map<String, Object> attributes = Map.of("accountId", accountId);
            Authentication auth = Authentication.build(String.valueOf(accountId), rolesList, attributes);

            return Publishers.just(auth);
        } catch (Exception ex) {
            return Publishers.empty();
        }
    }

}
