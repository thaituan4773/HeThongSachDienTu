package com.ddtt.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ddtt.dtos.BookCreateDTO;
import com.ddtt.dtos.BookDTO;
import com.ddtt.dtos.BookFullDetailDTO;
import com.ddtt.dtos.BookSummaryDTO;
import com.ddtt.dtos.CategoryDTO;
import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.repositories.BookRepository;
import com.ddtt.repositories.GenreRepository;
import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final GenreRepository genreRepository;
    private final Cloudinary cloudinary;
    private final int limit = 30;
    private final int pageSize = 12;

    public List<BookDTO> getAllBooks() {
        return bookRepository.findAllBooks();
    }

    @Cacheable("category-preview-trending")
    public CategoryDTO findTrendingBooks(int days) {
        List<BookDTO> books = bookRepository.findTrendingBooks(days, limit);
        return new CategoryDTO("trending", "Thịnh hành", books);
    }

    @Cacheable("category-preview-newest")
    public CategoryDTO findNewestBooks() {
        List<BookDTO> books = bookRepository.findNewestBooks(limit);
        return new CategoryDTO("newest", "Mới nhất", books);
    }

    @Cacheable("books-by-genre")
    public List<BookDTO> findBooksByGenreCached(int genreId) {
        return bookRepository.findBooksByGenre(genreId, limit);
    }

    public List<CategoryDTO> randomGenreCategories(int count) {
        List<Integer> genreIds = genreRepository.pickRandomGenreIds(count);
        return genreIds.stream()
                .map(id -> new CategoryDTO(
                "genre_" + id,
                genreRepository.getGenreName(id),
                findBooksByGenreCached(id)))
                .toList();
    }

    @Cacheable("category-preview-toprated")
    public CategoryDTO findTopRatedBooks() {
        List<BookDTO> books = bookRepository.findTopRatedBooks(limit);
        return new CategoryDTO("topRated", "Đánh giá cao nhất", books);
    }

    public BookFullDetailDTO getBookDetail(int bookId, int accountId) {
        return bookRepository.getBookDetail(bookId, accountId);
    }

    public PageResponseDTO<BookSummaryDTO> searchBooks(String kw, int page) {
        return bookRepository.searchBooks(kw, page, pageSize);
    }

    public PageResponseDTO<BookSummaryDTO> findBooksByGenrePaged(int genreId, int page, String sort) {
        if (sort == null || sort.isBlank()) {
            sort = "trending";
        }
        return bookRepository.getBooksByGenrePaged(genreId, page, pageSize, sort);
    }

    public PageResponseDTO<BookSummaryDTO> findBooksByAuthorPaged(int authorId, int page, boolean isAuthor) {
        return bookRepository.getBooksByAuthorPaged(authorId, page, pageSize, isAuthor);
    }

    public BookSummaryDTO createBook(BookCreateDTO dto, int authorId, CompletedFileUpload file) {
        if (file != null && file.getSize() > 0) {
            try {
                Map res = cloudinary.uploader()
                        .upload(file.getBytes(), ObjectUtils.asMap("resource_type", "auto"));
                dto.setCoverImageURL(res.get("secure_url").toString());
            } catch (IOException ex) {
                throw new RuntimeException("Lỗi upload ảnh lên Cloudinary", ex);
            }
        }
        return bookRepository.createBook(dto, authorId);
    }

}
