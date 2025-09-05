package com.ddtt.services;

import com.ddtt.dtos.ChapterContentDTO;
import com.ddtt.dtos.ChapterOverviewDTO;
import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.exceptions.ForbiddenException;
import com.ddtt.repositories.ChapterRepository;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final int pageSize = 32;

    public ChapterContentDTO readChapter(int chapterId, int accountId) {
        if (!chapterRepository.hasChapterAccess(chapterId, accountId)) {
            throw new ForbiddenException("Không có quyền xem chương");
        }
        return chapterRepository.readChapterContent(chapterId, accountId);
    }

    public PageResponseDTO<ChapterOverviewDTO> getChaptersInfoPaged(
            int bookId,
            int accountId,
            int page,
            boolean desc // true = chapter cuối, false = chapter đầu
    ) {
        return chapterRepository.getChaptersInfoPaged(bookId, accountId, page, pageSize, desc);
    }

    public void unlockChapter(int accountId, int chapterId) {
        if (chapterRepository.hasChapterAccess(chapterId, accountId)) {
            return;
        }
        chapterRepository.unlockChapter(accountId, chapterId);
    }

    public boolean checkAccess(int chapterId, int accountId) {
        return chapterRepository.hasChapterAccess(chapterId, accountId);
    }
}
