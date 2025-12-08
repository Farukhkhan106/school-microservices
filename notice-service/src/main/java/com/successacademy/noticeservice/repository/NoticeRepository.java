package com.successacademy.noticeservice.repository;

import com.successacademy.noticeservice.model.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findByActiveTrue();

    List<Notice> findByTitleContainingIgnoreCase(String keyword);

    List<Notice> findByStartDateBetween(LocalDate start, LocalDate end);
}
