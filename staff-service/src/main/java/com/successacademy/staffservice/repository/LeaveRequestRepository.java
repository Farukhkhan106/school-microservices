package com.successacademy.staffservice.repository;

import com.successacademy.staffservice.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByStaffIdOrderByCreatedAtDesc(Long staffId);

    List<LeaveRequest> findByStatusOrderByCreatedAtDesc(String status);

    @Query("SELECT l FROM LeaveRequest l WHERE l.staffId = :staffId AND " +
           "l.status IN ('PENDING', 'APPROVED') AND " +
           "(l.fromDate <= :toDate AND l.toDate >= :fromDate)")
    List<LeaveRequest> findOverlappingLeaves(
            @Param("staffId") Long staffId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("SELECT COUNT(l) FROM LeaveRequest l WHERE l.status = 'APPROVED' AND " +
           ":date BETWEEN l.fromDate AND l.toDate")
    long countApprovedOnLeaveForDate(@Param("date") LocalDate date);

    @Query("SELECT l FROM LeaveRequest l WHERE l.staffId = :staffId AND " +
           "l.status = 'APPROVED' AND " +
           "(l.fromDate <= :toDate AND l.toDate >= :fromDate)")
    List<LeaveRequest> findApprovedLeavesInDateRange(
            @Param("staffId") Long staffId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
