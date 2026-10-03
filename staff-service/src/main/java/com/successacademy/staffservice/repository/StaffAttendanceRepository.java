package com.successacademy.staffservice.repository;

import com.successacademy.staffservice.model.StaffAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffAttendanceRepository extends JpaRepository<StaffAttendance, Long> {

    Optional<StaffAttendance> findByStaffIdAndAttendanceDate(Long staffId, LocalDate attendanceDate);

    List<StaffAttendance> findByAttendanceDate(LocalDate attendanceDate);

    List<StaffAttendance> findByStaffIdOrderByAttendanceDateDesc(Long staffId);

    List<StaffAttendance> findByStaffIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            Long staffId, LocalDate from, LocalDate to
    );

    List<StaffAttendance> findByAttendanceDateBetweenOrderByAttendanceDateAsc(
            LocalDate from, LocalDate to
    );

    long countByAttendanceDateAndStatus(LocalDate date, String status);
}
