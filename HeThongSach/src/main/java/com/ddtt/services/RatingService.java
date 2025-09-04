package com.ddtt.services;

import com.ddtt.dtos.RatingDTO;
import com.ddtt.repositories.RatingRepository;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;

    public RatingDTO rateBook(RatingDTO dto, int accountId) {
        return ratingRepository.upsertRating(dto, accountId)
                .orElseThrow(() -> new IllegalArgumentException(
                "Chủ sách không thể đánh giá sách của chính họ"
        ));
    }

    public boolean deleteRating(int accountId, int bookId) {
        return ratingRepository.deleteRating(accountId, bookId);
    }

}
