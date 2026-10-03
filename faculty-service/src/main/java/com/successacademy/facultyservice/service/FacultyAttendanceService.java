package com.successacademy.facultyservice.service;

import com.successacademy.facultyservice.dto.FacultyAttendanceRequest;
import com.successacademy.facultyservice.dto.FacultyAttendanceResponse;

import java.time.LocalDate;
import java.util.List;

public interface FacultyAttendanceService {

    FacultyAttendanceResponse markAttendance(FacultyAttendanceRequest request, Long actorUserId);

    List<FacultyAttendanceResponse> bulkMarkAttendance(List<FacultyAttendanceRequest> requests, Long actorUserId);

    List<FacultyAttendanceResponse> getAttendanceByDate(LocalDate date, String department);

    List<FacultyAttendanceResponse> getAttendanceByMonth(int month, int year, String department);

    List<FacultyAttendanceResponse> getAttendanceForFaculty(Long facultyId, LocalDate from, LocalDate to);
}
