package com.ddtt.controllers;

import com.ddtt.dtos.CoinHistoryDTO;
import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.services.DonationService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.security.authentication.Authentication;
import jakarta.validation.constraints.Min;
import java.util.List;
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
    
    @Get("/me/coin-earned")
    public HttpResponse<PageResponseDTO<CoinHistoryDTO>> getCoinEarnedHistory(
            Authentication authentication,
            @QueryValue(value = "page", defaultValue = "1") @Min(value = 1, message = "page phải >= 1") int page
    ){
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        return HttpResponse.ok(donationService.getCoinEarnedHistory(accountId, page));
    }
    
    @Get("/me/coin-spent")
    public HttpResponse<PageResponseDTO<CoinHistoryDTO>> getCoinSpentHistory(
            Authentication authentication,
            @QueryValue(value = "page", defaultValue = "1") @Min(value = 1, message = "page phải >= 1") int page
    ){
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        return HttpResponse.ok(donationService.getCoinSpentHistory(accountId, page));
    }
}
