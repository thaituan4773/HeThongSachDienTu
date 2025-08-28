package com.ddtt.services;

import com.ddtt.dtos.ChapterContentDTO;
import com.ddtt.exceptions.NotFoundException;
import com.ddtt.repositories.ChapterRepository;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class ChapterService {

    private final ChapterRepository chapterRepository;

    public ChapterContentDTO readChapter(int chapterId, int accountId) {
        Boolean access = chapterRepository.hasChapterAccess(chapterId, accountId);
        if (access != true) {
            throw new NotFoundException("Không tìm thấy chương");
        }
        return chapterRepository.readChapterContent(chapterId, accountId);
    }

}
