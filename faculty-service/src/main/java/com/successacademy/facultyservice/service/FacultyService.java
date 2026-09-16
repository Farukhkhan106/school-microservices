package com.successacademy.facultyservice.service;

import com.successacademy.facultyservice.dto.AssignmentRequest;
import com.successacademy.facultyservice.dto.AssignmentResponse;
import com.successacademy.facultyservice.dto.FacultyRequest;
import com.successacademy.facultyservice.dto.FacultyResponse;
import com.successacademy.facultyservice.dto.ScheduleRequest;
import com.successacademy.facultyservice.dto.ScheduleResponse;
import com.successacademy.facultyservice.dto.TeacherProfileResponse;

import java.util.List;

public interface FacultyService {

    FacultyResponse addFaculty(FacultyRequest request);

    FacultyResponse updateFaculty(Long id, FacultyRequest request);

    void deleteFaculty(Long id);

    List<FacultyResponse> getAllFaculty();

    List<com.successacademy.facultyservice.dto.PublicFacultyResponse> getActiveFaculty();    // public website

    FacultyResponse getFacultyById(Long id);

    FacultyResponse getFacultyByUserId(Long userId);

    List<FacultyResponse> searchByName(String keyword);

    List<FacultyResponse> filterBySubject(String subject);

    String uploadPhoto(Long id, org.springframework.web.multipart.MultipartFile file);

    String uploadGeneralPhoto(org.springframework.web.multipart.MultipartFile file);

    // ── TEACHER MANAGEMENT FOUNDATION ────────────────────────────

    /** Full profile (faculty + assignments + schedule + allowed classes) for the LOGGED-IN teacher. */
    TeacherProfileResponse getTeacherProfile(Long authUserId);

    // Class Teacher (one per class-section; replace requires explicit flag)
    FacultyResponse assignClassTeacher(Long teacherId, String classTeacherOf, boolean replace);

    // Subject-teacher assignments
    List<AssignmentResponse> getAllAssignments();

    List<AssignmentResponse> getAssignmentsByTeacher(Long teacherId);

    AssignmentResponse addAssignment(AssignmentRequest request);

    AssignmentResponse updateAssignment(Long id, AssignmentRequest request);

    void deleteAssignment(Long id);

    // Weekly timetable
    List<ScheduleResponse> getAllSchedules();

    List<ScheduleResponse> getSchedulesByClass(String studentClass, String section);

    List<ScheduleResponse> getSchedulesByTeacher(Long teacherId);

    ScheduleResponse addSchedule(ScheduleRequest request);

    ScheduleResponse updateSchedule(Long id, ScheduleRequest request);

    void deleteSchedule(Long id);

    // Access checks (used by student-service & attendance-service for authorization)
    List<String> getAllowedClassSections(Long authUserId);

    boolean hasAnyAccess(Long authUserId, String studentClass, String section);

    boolean hasClassTeacherAccess(Long authUserId, String studentClass, String section);

    java.util.Map<String, Object> checkDeactivation(Long id);

    java.util.Map<String, String> getAllClassTeachers();

    // ── TEACHER ABSENCE & SUBSTITUTE WORKFLOW ────────────────────
    com.successacademy.facultyservice.dto.SubstituteResponse markAbsenceAndAssignSubstitute(com.successacademy.facultyservice.dto.SubstituteRequest request);

    void cancelSubstitute(Long id);

    List<com.successacademy.facultyservice.dto.SubstituteResponse> getSubstitutesByDate(java.time.LocalDate date);

    List<com.successacademy.facultyservice.dto.SubstituteResponse> getSubstitutesForTeacher(Long teacherId);

    com.successacademy.facultyservice.dto.AbsenceOverviewResponse getAbsenceOverview(java.time.LocalDate date);

    List<com.successacademy.facultyservice.dto.SubstituteResponse> getMySubstitutions(Long authUserId, java.time.LocalDate date);
}
