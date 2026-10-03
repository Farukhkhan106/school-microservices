package com.successacademy.staffservice.service;

import com.successacademy.staffservice.dto.StaffAttendanceRequest;
import com.successacademy.staffservice.dto.StaffAttendanceResponse;

import java.time.LocalDate;
import java.util.List;

public interface StaffAttendanceService {

    StaffAttendanceResponse markAttendance(StaffAttendanceRequest request, Long actorUserId);

    List<StaffAttendanceResponse> bulkMarkAttendance(List<StaffAttendanceRequest> requests, Long actorUserId);

    List<StaffAttendanceResponse> getAttendanceByDate(LocalDate date, Long departmentId);

    List<StaffAttendanceResponse> getAttendanceByMonth(int month, int year, Long departmentId);

    List<StaffAttendanceResponse> getAttendanceForStaff(Long staffId, LocalDate from, LocalDate to);

    StaffAttendanceResponse selfCheckIn(Long staffId, String remarks);

    StaffAttendanceResponse selfCheckOut(Long staffId, String remarks);

    StaffAttendanceResponse getTodayAttendance(Long staffId);
}
