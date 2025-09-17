package com.ddtt.services;

import com.ddtt.dtos.ChapterContentDTO;
import com.ddtt.dtos.ChapterEditDTO;
import com.ddtt.dtos.ChapterInputDTO;
import com.ddtt.dtos.ChapterOverviewDTO;
import com.ddtt.dtos.ChapterUpdateDTO;
import com.ddtt.dtos.CurrentReadingDTO;
import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.exceptions.ForbiddenException;
import com.ddtt.repositories.ChapterRepository;
import jakarta.inject.Singleton;
import java.math.BigDecimal;
import java.util.List;
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

    public boolean addChapter(int accountId, int bookId, ChapterInputDTO dto) {
        chapterRepository.addChapter(accountId, bookId, dto);
        return true;
    }

    public void updateChapter(int accountId, int chapterId, ChapterUpdateDTO dto) {
        chapterRepository.updateChapter(accountId, chapterId, dto);
    }

    public int getChapterPrice(int chapterId) {
        return chapterRepository.getChapterPrice(chapterId);
    }

    public List<ChapterEditDTO> getChaptersForEdit(int accountId, int bookId) {
        return chapterRepository.getChaptersForEdit(accountId, bookId);
    }

    public String getChapterContent(int accountId, int chapterId) {
        return chapterRepository.getChapterContent(accountId, chapterId);
    }
    
    public void updateReadingProgress(int accountId, int chapterId, BigDecimal progressPercent){
        chapterRepository.updateReadingProgress(accountId, chapterId, progressPercent);
    }
    
    public void softDeleteChapter(int accountId, int chapterId){
        chapterRepository.softDeleteChapter(accountId, chapterId);
    }
    
    public void markChaptersAsRead(int accountId, List<Integer> chapterIds){
        chapterRepository.markChaptersAsRead(accountId, chapterIds);
    }
    
    public void unmarkChaptersAsRead(int accountId, List<Integer> chapterIds) {
        chapterRepository.unmarkChaptersAsRead(accountId, chapterIds);
    }
    
    public void clearReadingProgress(int accountId, List<Integer> bookIds) {
        chapterRepository.clearReadingProgress(accountId, bookIds);
    }
    
    public List<CurrentReadingDTO> getCurrentlyReadingBooks(int accountId) {
        return chapterRepository.getCurrentlyReadingBooks(accountId);
    }
}
