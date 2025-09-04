package com.ddtt.controllers;

import com.ddtt.services.PurchaseCoinsService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.authentication.Authentication;
import jakarta.validation.constraints.NotEmpty;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;

@Controller("/api")
@RequiredArgsConstructor
public class ApiPurchaseCoins {

    private final PurchaseCoinsService purchaseCoinsService;

    @Post("/payments")
    public Publisher<Map<String,Object>> checkout(
            @Body("coinPackId") int coinPackId,
            @Body("paymentMethod") @NotEmpty String paymentMethod,
            Authentication authentication
    ) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        return purchaseCoinsService.createTransaction(accountId, coinPackId, paymentMethod);
    }

    @Post("/payments/momo/ipn")
    public HttpResponse<String> receiveIpn(@Body Map<String, Object> payload)
            throws InvalidKeyException, NoSuchAlgorithmException {
        purchaseCoinsService.verifyPayment(payload);
        return HttpResponse.noContent();
    }

}
