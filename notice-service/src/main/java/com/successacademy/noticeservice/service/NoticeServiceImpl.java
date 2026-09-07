package com.successacademy.noticeservice.service;

import com.successacademy.noticeservice.model.Notice;
import com.successacademy.noticeservice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeRepository repository;

    @Override
    public Notice addNotice(Notice notice) {
        if (notice.getPublishedAt() == null) notice.setPublishedAt(LocalDate.now());
        if (notice.getStatus() == null) notice.setStatus("Published");
        return repository.save(notice);
    }

    @Override
    public Notice updateNotice(Long id, Notice notice) {
        Notice existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notice not found with id: " + id));
        existing.setTitle(notice.getTitle());
        existing.setContent(notice.getContent());
        existing.setCategory(notice.getCategory());
        existing.setStatus(notice.getStatus());
        existing.setPublicVisible(notice.isPublicVisible());
        existing.setActive(notice.isActive());
        existing.setPublishedAt(notice.getPublishedAt());
        existing.setExpiresAt(notice.getExpiresAt());
        existing.setCreatedBy(notice.getCreatedBy());
        existing.setStartDate(notice.getStartDate());
        existing.setEndDate(notice.getEndDate());
        return repository.save(existing);
    }

    @Override
    public void deleteNotice(Long id) { repository.deleteById(id); }

    @Override
    public List<Notice> getAllNotices() {
        return repository.findAll().stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId())).toList();
    }

    @Override
    public List<Notice> getActiveNotices() {
        return repository.findByActiveTrue().stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId())).toList();
    }

    @Override
    public List<Notice> getPublicNotices() {
        return repository.findByPublicVisibleTrueAndStatusAndActiveTrue("Published").stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId())).toList();
    }

    @Override
    public List<Notice> getPublicNoticesByCategory(String category) {
        return repository.findByPublicVisibleTrueAndStatusAndActiveTrueAndCategoryIgnoreCase("Published", category).stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId())).toList();
    }

    @Override
    public List<Notice> getNoticesByCategory(String category) {
        return repository.findByCategoryIgnoreCase(category).stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId())).toList();
    }

    @Override
    public Notice getNoticeById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notice not found with id: " + id));
    }

    @Override
    public List<Notice> searchByTitle(String keyword) {
        return repository.findByTitleContainingIgnoreCase(keyword);
    }

    @Override
    public List<Notice> filterByDateRange(LocalDate start, LocalDate end) {
        return repository.findByStartDateBetween(start, end);
    }

    @Override
    public Notice toggleActive(Long id, boolean active) {
        Notice notice = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notice not found with id: " + id));
        notice.setActive(active);
        return repository.save(notice);
    }
}
