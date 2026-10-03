package com.successacademy.staffservice.repository;

import com.successacademy.staffservice.model.StaffShiftAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffShiftAssignmentRepository extends JpaRepository<StaffShiftAssignment, Long> {

    List<StaffShiftAssignment> findByStaffIdOrderByEffectiveFromDesc(Long staffId);

    @Query("SELECT s FROM StaffShiftAssignment s WHERE s.staffId = :staffId AND " +
           "s.effectiveFrom <= :date AND (s.effectiveTo IS NULL OR s.effectiveTo >= :date)")
    Optional<StaffShiftAssignment> findActiveShiftForDate(@Param("staffId") Long staffId, @Param("date") LocalDate date);

    long countByShiftIdAndEffectiveToIsNull(Long shiftId);
}
