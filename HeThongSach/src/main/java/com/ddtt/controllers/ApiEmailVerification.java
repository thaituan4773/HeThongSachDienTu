package com.ddtt.controllers;

import com.ddtt.services.EmailService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import lombok.RequiredArgsConstructor;

@Controller("/api")
@RequiredArgsConstructor
public class ApiEmailVerification {

    private final EmailService emailService;
    private static final String DEEPLINK = "mystore://login";

    @Get("/verify")
    public HttpResponse<String> verify(@QueryValue String token) throws Exception {
        emailService.verifyToken(token);
        // HTML hiển thị thông báo + tự động mở app
        String html = """
                      <!DOCTYPE html>
                      <html lang="vi">
                      <head>
                        <meta charset="UTF-8">
                        <title>Xác thực Email</title>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <style>
                          body {
                            font-family: Roboto,Arial,sans-serif;
                            text-align: center;
                            padding: 2rem 1rem;
                            background: #f9fafb;
                            color: #111827;
                          }
                          h2 {
                            font-size: 1.5rem;
                            margin-bottom: 1rem;
                            color: #16a34a;
                          }
                          p {
                            font-size: 1rem;
                            margin-bottom: 1rem;
                          }
                        </style>
                      </head>
                      <body>
                        <h2>Email xác thực thành công!</h2>
                        <p>Bạn sẽ được chuyển về ứng dụng ngay...</p>
                        <script>
                          setTimeout(function() {
                            window.location.href = "%s";
                          }, 1500);
                        </script>
                      </body>
                      </html>
                      """.formatted(DEEPLINK);

        return HttpResponse.ok(html).contentType(MediaType.TEXT_HTML);
    }
}
