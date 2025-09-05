package com.ddtt.controllers;

import com.ddtt.services.DonationService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.security.authentication.Authentication;
import lombok.RequiredArgsConstructor;

@Controller("/api")
@RequiredArgsConstructor
public class ApiDonation {

    private final DonationService donationService;

    @Post("/books/{bookId}/donate")
    public HttpResponse<Boolean> createDonation(
            @PathVariable int bookId,
            Authentication authentication,
            @QueryValue int amount
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        return HttpResponse.created(donationService.createDonation(accountId, bookId, amount));
    }
}
