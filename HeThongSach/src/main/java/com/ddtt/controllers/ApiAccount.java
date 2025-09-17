package com.ddtt.controllers;

import com.ddtt.dtos.ResetPasswordFormDTO;
import com.ddtt.dtos.AccountDTO;
import com.ddtt.dtos.AccountPatchDTO;
import com.ddtt.dtos.BookInputDTO;
import com.ddtt.dtos.BookSummaryAuthorDTO;
import com.ddtt.dtos.BookSummaryDTO;
import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.services.AccountService;
import com.ddtt.services.BookService;
import com.ddtt.services.EmailService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Part;
import io.micronaut.http.annotation.Patch;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.security.authentication.Authentication;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Controller("/api")
@RequiredArgsConstructor
public class ApiAccount {

    private final BookService bookService;
    private final AccountService accountService;
    private final EmailService emailService;

    @Get("/me/books")
    public HttpResponse<PageResponseDTO<BookSummaryAuthorDTO>> getMyBooks(
            @QueryValue(value = "page", defaultValue = "1") @Min(value = 1, message = "page phải >= 1") int page,
            Authentication authentication
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        return HttpResponse.ok(bookService.getMyBooks(accountId, page));
    }

    @Post(value = "/me/books", consumes = "multipart/form-data")
    public HttpResponse<BookSummaryAuthorDTO> addBook(
            @Part("title") @NotBlank(message = "Yêu cầu tựa đề") String title,
            @Nullable @Part("description") String description,
            @Part("genreId") @Min(value = 1, message = "Yêu cầu chọn thể loại") int genreId,
            @Nullable @Size(max = 10) @Part("tags") List<String> tags,
            @Nullable @Part("coverImage") CompletedFileUpload file,
            Authentication authentication
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        BookInputDTO book = new BookInputDTO();
        book.setTitle(title);
        book.setDescription(description);
        book.setGenreId(genreId);
        book.setTags(tags);
        book.setStatus("DRAFT");
        return HttpResponse.created(bookService.createBook(book, accountId, file));
    }

    @Get("/users/{accountId}")
    public HttpResponse<AccountDTO> getProfile(@PathVariable int accountId) {
        return HttpResponse.ok(accountService.getAccountById(accountId));
    }

    @Get("/me")
    public HttpResponse<AccountDTO> getMyProfile(Authentication authentication) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        return HttpResponse.ok(accountService.getAccountById(accountId));
    }

    @Get("/me/balance")
    public HttpResponse<Integer> getBalance(Authentication authentication) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        return HttpResponse.ok(accountService.getBalance(accountId));
    }

    @Get("/users/{authorId}/books")
    public HttpResponse<PageResponseDTO<BookSummaryDTO>> getBooksByUser(
            @QueryValue(value = "page", defaultValue = "1") @Min(value = 1, message = "page phải >= 1") int page,
            @PathVariable int authorId
    ) {
        return HttpResponse.ok(bookService.findBooksByAuthorPaged(authorId, page));
    }

    @Post("/change-email")
    public HttpResponse changeEmailRequest(
            Authentication authentication,
            @Body("newEmail") String newEmail
    ) throws Exception {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        emailService.requestEmailChange(accountId, newEmail);
        return HttpResponse.ok();
    }

    @Get("/change-email/confirm")
    public HttpResponse<String> confirmEmailChange(@QueryValue String token) {
        try {
            emailService.confirmEmailChange(token);

            String html = """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8">
              <title>Đổi email thành công</title>
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
              <h2>Email đã được thay đổi thành công!</h2>
              <p>Bạn sẽ được chuyển về ứng dụng ngay...</p>
              <script>
                setTimeout(function() {
                  window.location.href = "mystore://login";
                }, 1500);
              </script>
            </body>
            </html>
        """;

            return HttpResponse.ok(html).contentType(MediaType.TEXT_HTML);
        } catch (Exception e) {
            String html = """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8">
              <title>Đổi email thất bại</title>
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <style>
                body {
                  font-family: Roboto,Arial,sans-serif;
                  text-align: center;
                  padding: 2rem 1rem;
                  background: #f9fafb;
                  color: #dc2626;
                }
                h2 {
                  font-size: 1.5rem;
                  margin-bottom: 1rem;
                  color: #dc2626;
                }
                p {
                  font-size: 1rem;
                  margin-bottom: 1rem;
                }
              </style>
            </head>
            <body>
              <h2>Liên kết không hợp lệ hoặc đã hết hạn</h2>
              <p>Vui lòng thử yêu cầu đổi email lại.</p>
              <script>
                setTimeout(function() {
                  window.location.href = "mystore://settings";
                }, 2500);
              </script>
            </body>
            </html>
        """;

            return HttpResponse.ok(html).contentType(MediaType.TEXT_HTML);
        }
    }

    @Post("/forgot-password")
    public HttpResponse forgotPasswordRequest(@Body("email") String email) throws Exception {
        accountService.createForgotPasswordResetRequest(email);
        return HttpResponse.ok();
    }

    @Get("/forgot-password/confirm")
    public HttpResponse<?> forgotPasswordWeb(@QueryValue String token) {

        return HttpResponse.redirect(
                URI.create(token).create("mystore://forgot-password-confirm?token=" + token)
        );
    }

    @Post("/forgot-password/confirm")
    public HttpResponse<String> resetPassword(@Body ResetPasswordFormDTO form) throws Exception {
        System.out.println("resetPassword form token=" + form.getToken() + " password=" + form.getNewPassword());
        accountService.confirmForgotPasswordRequest(form.getToken(), form.getNewPassword());
        return HttpResponse.ok();
    }

    @Post(value = "/change-password", consumes = "multipart/form-data")
    public HttpResponse changePassword(
            @Part("oldPassword") @NotBlank String oldPassword,
            @Part("newPassword") @NotBlank String newPassword,
            Authentication authentication
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        accountService.changePassword(accountId, oldPassword, newPassword);
        return HttpResponse.ok();
    }

    @Patch(value = "/change-info", consumes = "multipart/form-data")
    public HttpResponse changeInfo(
            @Nullable @Part("displayName") @NotBlank @Size(max = 30, message = "Display name không được vượt quá 30 ký tự") String displayName,
            @Nullable @Part("bio") String bio,
            @Nullable @Part("avatar") CompletedFileUpload file,
            Authentication authentication
    ) {
        int accountId = (Integer) authentication.getAttributes().get("accountId");
        AccountPatchDTO dto = new AccountPatchDTO();
        dto.setDisplayName(displayName);
        dto.setBio(bio);
        accountService.patchAccount(accountId, dto, file);
        return HttpResponse.ok();
    }

}
