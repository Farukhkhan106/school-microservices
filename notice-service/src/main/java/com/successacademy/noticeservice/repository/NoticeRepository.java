package com.successacademy.noticeservice.repository;

import com.successacademy.noticeservice.model.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findByActiveTrue();

    // publicVisible=true, status=Published, active=true
    List<Notice> findByPublicVisibleTrueAndStatusAndActiveTrue(String status);

    // category filter
    List<Notice> findByCategoryIgnoreCase(String category);

    // public + category
    List<Notice> findByPublicVisibleTrueAndStatusAndActiveTrueAndCategoryIgnoreCase(
            String status, String category);

    List<Notice> findByTitleContainingIgnoreCase(String keyword);

    List<Notice> findByStartDateBetween(LocalDate start, LocalDate end);
}
