package com.successacademy.staffservice.repository;

import com.successacademy.staffservice.model.StaffDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffDocumentRepository extends JpaRepository<StaffDocument, Long> {

    List<StaffDocument> findByStaffIdOrderByCreatedAtDesc(Long staffId);
}
