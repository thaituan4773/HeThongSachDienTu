package com.ddtt.services;

import com.ddtt.dtos.BookDTO;
import com.ddtt.dtos.CategoryDTO;
import com.ddtt.repositories.BookRepository;
import com.ddtt.repositories.GenreRepository;
import com.ddtt.repositories.TagRepository;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;


@Singleton
@RequiredArgsConstructor
public class RecommendationService {

    private final BookRepository bookRepository;
    private final GenreRepository genreRepository;
    private final TagRepository tagRepository;

    private final int limit = 30;
    private final double alpha = 0.7;          // weight của genre
    private final double beta = 0.3;           // weight của tag
    private final double explorationRate = 0.1;

    public CategoryDTO recommendBooksForUserWithTags(int userId) {

        Map<Integer, Integer> genreCounts = genreRepository.countGenresByUser(userId, 30);
        Map<Integer, Integer> tagCounts = tagRepository.countTagsByUser(userId, 30);

        if (genreCounts.isEmpty() && tagCounts.isEmpty()) {
            List<BookDTO> trending = bookRepository.findTrendingBooks(7, limit);
            return new CategoryDTO("recommended", "Dành cho bạn", trending);
        }

        Map<Integer, Double> genreProbs = normalizeCounts(genreCounts);
        Map<Integer, Double> tagProbs = normalizeCounts(tagCounts);

        // Bổ sung keys chưa từng xuất hiện (xác suất 0)
        for (Integer g : genreRepository.getAllGenreId()) {
            genreProbs.putIfAbsent(g, 0.0);
        }
        for (Integer t : tagRepository.getAllTagIds()) {
            tagProbs.putIfAbsent(t, 0.0);
        }

        // Combine
        Map<String, Double> combined = new HashMap<>(genreProbs.size() + tagProbs.size());
        if (!genreCounts.isEmpty()) {
            genreProbs.forEach((k, v) -> combined.put("G:" + k, v * alpha));
        }
        if (!tagCounts.isEmpty()) {
            tagProbs.forEach((k, v) -> combined.put("T:" + k, v * beta));
        }

        // Trường hợp một bên rỗng, có thể scale lại để tổng = (alpha hoặc beta) < 1 → phần còn lại do exploration bù
        // Hoặc chuẩn hoá lại ở đây nếu muốn tổng (trước exploration) = 1:
        double preSum = combined.values().stream().mapToDouble(Double::doubleValue).sum();
        if (preSum > 0) {
            combined.replaceAll((k, v) -> v / preSum); // giờ tổng = 1 trước khi add exploration
        }

        // Exploration: phân bổ e đều + (1 - e) * distribution cũ
        if (!combined.isEmpty()) {
            double explorationPerKey = explorationRate / combined.size();
            combined.replaceAll((k, v) -> v * (1 - explorationRate) + explorationPerKey);
        }

        // Normalize sau exploration (đảm bảo sum = 1 do sai số double)
        double finalSum = combined.values().stream().mapToDouble(Double::doubleValue).sum();
        if (finalSum > 0) {
            combined.replaceAll((k, v) -> v / finalSum);
        }

        List<Map.Entry<String, Double>> distribution = combined.entrySet()
                .stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        List<BookDTO> result = new ArrayList<>(limit);
        Set<Integer> chosenBookIds = new HashSet<>();
        int attempts = 0;
        int maxAttempts = limit * 6;

        while (result.size() < limit && attempts < maxAttempts && !distribution.isEmpty()) {
            attempts++;
            String key = pickKeyByWeight(distribution);
            if (key.startsWith("G:")) {
                int genreId = Integer.parseInt(key.substring(2));
                bookRepository.findRandomBookByGenreExcludingUser(genreId, userId).ifPresent(b -> {
                    if (chosenBookIds.add(b.getBookId())) {
                        result.add(b);
                    }
                });
            } else {
                int tagId = Integer.parseInt(key.substring(2));
                bookRepository.findRandomBookByTagExcludingUser(tagId, userId).ifPresent(b -> {
                    if (chosenBookIds.add(b.getBookId())) {
                        result.add(b);
                    }
                });
            }
        }

        if (result.size() < limit) {
            List<BookDTO> trending = bookRepository.findTrendingBooks(7, limit * 2);
            for (BookDTO b : trending) {
                if (result.size() >= limit) break;
                if (chosenBookIds.add(b.getBookId())) {
                    result.add(b);
                }
            }
        }

        return new CategoryDTO("recommended", "Dành cho bạn", result);
    }

    private Map<Integer, Double> normalizeCounts(Map<Integer, Integer> counts) {
        Map<Integer, Double> out = new HashMap<>();
        int total = counts.values().stream().mapToInt(i -> i).sum();
        if (total == 0) {
            counts.keySet().forEach(k -> out.put(k, 0.0));
            return out;
        }
        counts.forEach((k, v) -> out.put(k, v / (double) total));
        return out;
    }

    private String pickKeyByWeight(List<Map.Entry<String, Double>> distribution) {
        double r = ThreadLocalRandom.current().nextDouble();
        double cumulative = 0.0;
        for (var e : distribution) {
            cumulative += e.getValue();
            if (r <= cumulative) {
                return e.getKey();
            }
        }
        return distribution.get(distribution.size() - 1).getKey();
    }
}