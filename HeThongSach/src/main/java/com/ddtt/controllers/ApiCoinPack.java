package com.ddtt.controllers;

import com.ddtt.dtos.CoinPackDTO;
import com.ddtt.services.CoinPackService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Controller("/api")
@RequiredArgsConstructor
public class ApiCoinPack {

    private final CoinPackService coinPackService;

    @Get("/coin-packs")
    public HttpResponse<List<CoinPackDTO>> getPacks() {
        return HttpResponse.ok(coinPackService.getAllCoinPackInfo());
    }
}
