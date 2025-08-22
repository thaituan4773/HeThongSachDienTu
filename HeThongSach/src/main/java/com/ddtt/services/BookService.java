package com.ddtt.services;

import com.ddtt.dtos.BookDTO;
import com.ddtt.dtos.CategoryDTO;
import com.ddtt.repositories.BookRepository;
import com.ddtt.repositories.GenreRepository;
import io.micronaut.cache.annotation.Cacheable;
import jakarta.inject.Singleton;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final GenreRepository genreRepository;
    private final int limit = 30;

    public List<BookDTO> getAllBooks() {
        return bookRepository.findAllBooks();
    }

    @Cacheable("categoryPreview")
    public CategoryDTO findTrendingBooks(int days) {
        List<BookDTO> books = bookRepository.findTrendingBooks(days, limit);
        return new CategoryDTO("trending", "Thịnh hành", books);
    }

    @Cacheable("categoryPreview")
    public CategoryDTO findNewestBooks(int limit) {
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
    
    @Cacheable("categoryPreview")
    public CategoryDTO findTopRatedBooks(){
        List<BookDTO> books = bookRepository.findTopRatedBooks(limit);
        return new CategoryDTO("topRated", "Đánh giá cao nhất", books);
    }
    

}
