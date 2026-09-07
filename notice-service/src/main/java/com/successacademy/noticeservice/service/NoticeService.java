package com.successacademy.noticeservice.service;

import com.successacademy.noticeservice.model.Notice;

import java.time.LocalDate;
import java.util.List;

public interface NoticeService {

    Notice addNotice(Notice notice);

    Notice updateNotice(Long id, Notice notice);

    void deleteNotice(Long id);

    List<Notice> getAllNotices();

    List<Notice> getActiveNotices();

    // Public website — isPublic=true + status=Published + active=true
    List<Notice> getPublicNotices();

    // Public + filtered by category
    List<Notice> getPublicNoticesByCategory(String category);

    // Admin/internal — filter by category
    List<Notice> getNoticesByCategory(String category);

    Notice getNoticeById(Long id);

    List<Notice> searchByTitle(String keyword);

    List<Notice> filterByDateRange(LocalDate start, LocalDate end);

    Notice toggleActive(Long id, boolean active);
}
