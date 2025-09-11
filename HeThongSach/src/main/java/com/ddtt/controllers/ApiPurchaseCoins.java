package com.ddtt.controllers;

import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.dtos.PurchaseHistoryDTO;
import com.ddtt.services.PurchaseCoinsService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.security.authentication.Authentication;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;

@Controller("/api")
@RequiredArgsConstructor
public class ApiPurchaseCoins {

    private final PurchaseCoinsService purchaseCoinsService;
    private final String DEEPLINK = "mystore://CoinStore";

    @Post("/payments")
    public Publisher<Map<String, Object>> checkout(
            @Body("coinPackId") int coinPackId,
            @Body("paymentMethod") @NotEmpty String paymentMethod,
            Authentication authentication
    ) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        return purchaseCoinsService.createTransaction(accountId, coinPackId, paymentMethod);
    }

    @Get("/me/coin-purchases")
    public HttpResponse<PageResponseDTO<PurchaseHistoryDTO>> getCoinSpentHistory(
            Authentication authentication,
            @QueryValue(value = "page", defaultValue = "1") @Min(value = 1, message = "page phải >= 1") int page
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        return HttpResponse.ok(purchaseCoinsService.getPurchaseHistory(accountId, page));
    }

    @Get("/payments/result")
    public HttpResponse<String> result(@QueryValue("orderId") UUID orderId, @QueryValue("resultCode") int resultCode) {
        String status = (resultCode == 0) ? "SUCCESS" : "FAILED";
        purchaseCoinsService.updateStatus(orderId, status);
        if ("SUCCESS".equalsIgnoreCase(status)) {
            return HttpResponse.ok(successHtml()).contentType(MediaType.TEXT_HTML);
        } else {
            return HttpResponse.ok(failedHtml()).contentType(MediaType.TEXT_HTML);
        }
    }

    private String successHtml() {
        return """
        <!DOCTYPE html>
        <html lang="vi">
        <head>
          <meta charset="UTF-8" />
          <title>Thanh toán thành công</title>
          <meta name="viewport" content="width=device-width, initial-scale=1" />
          <meta name="theme-color" content="#16a34a" />
          <style>
            body{
               font-family:Roboto,Arial,sans-serif;
               margin:0;
               background:#f9fafb;
               color:#111827;
               text-align:center
            }
            .wrap{
               max-width:560px;
               margin:0 auto;
               padding:40px 16px
            }
            h1{
               font-size:22px;
               margin:0 0 12px;
               color:#16a34a
            }
            p{
               font-size:16px;
               margin:0 0 16px
            }
          </style>
        </head>
        <body>
          <div class="wrap">
            <h1>Thanh toán thành công!</h1>
            <p>Bạn sẽ được chuyển về ứng dụng ngay…</p>
          </div>
          <script>
            setTimeout(function(){ window.location.href = "%s"; }, 1200);
          </script>
        </body>
        </html>
        """.formatted(DEEPLINK);
    }

    private String failedHtml() {
        return """
        <!DOCTYPE html>
        <html lang="vi">
        <head>
          <meta charset="UTF-8" />
          <title>Thanh toán thất bại</title>
          <meta name="viewport" content="width=device-width, initial-scale=1" />
          <meta name="theme-color" content="#dc2626" />
          <style>
            body{
               font-family:Roboto,Arial,sans-serif;
               margin:0;
               background:#f9fafb;
               color:#111827;
               text-align:center
            }
            .wrap{
               max-width:560px;
               margin:0 auto;
               padding:40px 16px
            }
            h1{
               font-size:22px;
               margin:0 0 12px;
               color:#dc2626
            }
            p{
               font-size:16px;
               margin:0 0 16px
            }
          </style>
        </head>
        <body>
          <div class="wrap">
            <h1>Thanh toán thất bại</h1>
            <p>Rất tiếc, giao dịch của bạn chưa hoàn tất.</p>
          </div>
          <script>
            setTimeout(function(){ window.location.href = "%s"; }, 1500);
          </script>
        </body>
        </html>
        """.formatted(DEEPLINK);
    }

}
