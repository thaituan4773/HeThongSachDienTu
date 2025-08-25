package com.ddtt.services;

import com.ddtt.dtos.ChapterAccessDTO;
import com.ddtt.dtos.ChapterContentDTO;
import com.ddtt.exceptions.ForbiddenException;
import com.ddtt.exceptions.NotFoundException;
import com.ddtt.repositories.ChapterRepository;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class ChapterService {
    private final ChapterRepository chapterRepository;

    public ChapterContentDTO readChapter(int chapterId, Integer userId) {
        ChapterAccessDTO access = chapterRepository.getChapterAccess(chapterId, userId);
        if (access == null) {
            throw new NotFoundException("Không tìm thấy chương");
        }
        // chapter free
        if (access.getCoinPrice() == 0) {
            return chapterRepository.getChapterContent(chapterId);
        }
        // chapter trả phí
        if (Boolean.TRUE.equals(access.isAlreadyPurchased())) {
            return chapterRepository.getChapterContent(chapterId);
        }
        throw new ForbiddenException("Bạn cần mua chương này để đọc");
    }
    
}
