package com.successacademy.facultyservice.repository;

import com.successacademy.facultyservice.model.FacultyLeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FacultyLeaveRepository extends JpaRepository<FacultyLeaveRequest, Long> {

    List<FacultyLeaveRequest> findByFacultyIdOrderByCreatedAtDesc(Long facultyId);

    List<FacultyLeaveRequest> findByStatusOrderByCreatedAtDesc(String status);

    List<FacultyLeaveRequest> findAllByOrderByCreatedAtDesc();

    @Query("SELECT l FROM FacultyLeaveRequest l WHERE l.facultyId = :facultyId AND l.status = 'APPROVED' AND " +
           "((l.fromDate BETWEEN :start AND :end) OR (l.toDate BETWEEN :start AND :end) OR " +
           "(l.fromDate <= :start AND l.toDate >= :end))")
    List<FacultyLeaveRequest> findApprovedLeavesOverlapping(@Param("facultyId") Long facultyId,
                                                           @Param("start") LocalDate start,
                                                           @Param("end") LocalDate end);
}
