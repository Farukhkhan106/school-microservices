package com.successacademy.facultyservice.controller;

import com.successacademy.facultyservice.dto.AbsenceOverviewResponse;
import com.successacademy.facultyservice.dto.AssignmentRequest;
import com.successacademy.facultyservice.dto.AssignmentResponse;
import com.successacademy.facultyservice.dto.FacultyResponse;
import com.successacademy.facultyservice.dto.ScheduleRequest;
import com.successacademy.facultyservice.dto.ScheduleResponse;
import com.successacademy.facultyservice.dto.SubstituteRequest;
import com.successacademy.facultyservice.dto.SubstituteResponse;
import com.successacademy.facultyservice.dto.TeacherProfileResponse;
import com.successacademy.facultyservice.exception.UnauthorizedException;
import com.successacademy.facultyservice.security.UserContext;
import com.successacademy.facultyservice.service.FacultyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Class-teacher assignments, subject-teacher assignments and the weekly
 * timetable. ADMIN endpoints verify the X-User-Role header set by the gateway;
 * teacher endpoints resolve identity from X-User-Id — never from request body.
 */
@RestController
@RequestMapping("/faculty")
@RequiredArgsConstructor
public class TeacherAssignmentController {

    private final FacultyService service;

    // ── ADMIN: class teacher ─────────────────────────────────────

    /** Assign/replace/unassign the Class Teacher of a class-section.
     *  replace=false → 409 "Class 8-A is already assigned to X" if taken. */
    @PostMapping("/{id}/class-teacher")
    public FacultyResponse assignClassTeacher(@PathVariable Long id,
                                              @RequestParam String classTeacherOf,
                                              @RequestParam(defaultValue = "false") boolean replace,
                                              HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return service.assignClassTeacher(id, classTeacherOf, replace);
    }

    /** Returns currently active Class Teacher assignments { "8-A": "Priya Sharma", ... } */
    @GetMapping("/class-teachers")
    public Map<String, String> allClassTeachers(HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return service.getAllClassTeachers();
    }

    /** Pre-check assignments before deactivating a teacher */
    @GetMapping("/{id}/check-deactivation")
    public Map<String, Object> checkDeactivation(@PathVariable Long id, HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return service.checkDeactivation(id);
    }

    // ── ADMIN: subject-teacher assignments ───────────────────────

    @GetMapping("/assignments")
    public List<AssignmentResponse> allAssignments(HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return service.getAllAssignments();
    }

    @GetMapping("/assignments/teacher/{teacherId}")
    public List<AssignmentResponse> assignmentsByTeacher(@PathVariable Long teacherId, HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return service.getAssignmentsByTeacher(teacherId);
    }

    @PostMapping("/assignments")
    public AssignmentResponse addAssignment(@RequestBody AssignmentRequest r, HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return service.addAssignment(r);
    }

    @PutMapping("/assignments/{id}")
    public AssignmentResponse updateAssignment(@PathVariable Long id, @RequestBody AssignmentRequest r,
                                               HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return service.updateAssignment(id, r);
    }

    @DeleteMapping("/assignments/{id}")
    public void deleteAssignment(@PathVariable Long id, HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        service.deleteAssignment(id);
    }

    // ── ADMIN: weekly timetable ──────────────────────────────────

    @GetMapping("/schedule")
    public List<ScheduleResponse> allSchedules(HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return service.getAllSchedules();
    }

    @GetMapping("/schedule/class")
    public List<ScheduleResponse> classSchedule(@RequestParam String studentClass,
                                                @RequestParam String section,
                                                HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN", "TEACHER", "STUDENT");
        return service.getSchedulesByClass(studentClass, section);
    }

    @GetMapping("/schedule/teacher/{teacherId}")
    public List<ScheduleResponse> teacherSchedule(@PathVariable Long teacherId, HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return service.getSchedulesByTeacher(teacherId);
    }

    @PostMapping("/schedule")
    public ScheduleResponse addSchedule(@RequestBody ScheduleRequest r, HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return service.addSchedule(r);
    }

    @PutMapping("/schedule/{id}")
    public ScheduleResponse updateSchedule(@PathVariable Long id, @RequestBody ScheduleRequest r,
                                           HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return service.updateSchedule(id, r);
    }

    @DeleteMapping("/schedule/{id}")
    public void deleteSchedule(@PathVariable Long id, HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        service.deleteSchedule(id);
    }

    // ── ADMIN: teacher absence & substitute workflow ────────────

    @PostMapping("/substitutes")
    public SubstituteResponse assignSubstitute(@RequestBody SubstituteRequest r, HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return service.markAbsenceAndAssignSubstitute(r);
    }

    @GetMapping("/substitutes")
    public List<SubstituteResponse> getSubstitutes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return service.getSubstitutesByDate(date);
    }

    @GetMapping("/substitutes/teacher/{teacherId}")
    public List<SubstituteResponse> getSubstitutesByTeacher(@PathVariable Long teacherId, HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return service.getSubstitutesForTeacher(teacherId);
    }

    @DeleteMapping("/substitutes/{id}")
    public void cancelSubstitute(@PathVariable Long id, HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        service.cancelSubstitute(id);
    }

    @GetMapping("/substitutes/overview")
    public AbsenceOverviewResponse getAbsenceOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return service.getAbsenceOverview(date);
    }

    // ── TEACHER (identity ALWAYS from gateway headers) ───────────

    @GetMapping("/teacher/me")
    public TeacherProfileResponse myProfile(HttpServletRequest request) {
        UserContext.requireRole(request, "TEACHER", "ADMIN");
        return service.getTeacherProfile(requireUserId(request));
    }

    @GetMapping("/teacher/classes")
    public List<String> myClasses(HttpServletRequest request) {
        UserContext.requireRole(request, "TEACHER", "ADMIN");
        return service.getAllowedClassSections(requireUserId(request));
    }

    @GetMapping("/teacher/schedule")
    public List<ScheduleResponse> mySchedule(HttpServletRequest request) {
        UserContext.requireRole(request, "TEACHER", "ADMIN");
        return service.getSchedulesByTeacher(
                service.getTeacherProfile(requireUserId(request)).getFaculty().getId());
    }

    @GetMapping("/teacher/substitutions")
    public List<SubstituteResponse> mySubstitutions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        UserContext.requireRole(request, "TEACHER", "ADMIN");
        return service.getMySubstitutions(requireUserId(request), date);
    }

    /** Frontend uses this to know what the logged-in teacher may do for a class. */
    @GetMapping("/teacher/access")
    public Map<String, Object> myAccess(@RequestParam String studentClass,
                                        @RequestParam String section,
                                        HttpServletRequest request) {
        UserContext.requireRole(request, "TEACHER", "ADMIN");
        Long userId = requireUserId(request);
        boolean classTeacher = service.hasClassTeacherAccess(userId, studentClass, section);
        boolean anyAccess = classTeacher || service.hasAnyAccess(userId, studentClass, section);
        return Map.of(
                "studentClass", studentClass,
                "section", section,
                "canView", anyAccess,
                "canMarkAttendance", classTeacher
        );
    }

    // ── INTERNAL: service-to-service (student/attendance services) ──

    @GetMapping("/internal/allowed-classes")
    public List<String> internalAllowedClasses(@RequestParam Long userId) {
        return service.getAllowedClassSections(userId);
    }

    @GetMapping("/internal/has-access")
    public Map<String, Object> internalHasAccess(@RequestParam Long userId,
                                                 @RequestParam String studentClass,
                                                 @RequestParam String section) {
        boolean classTeacher = service.hasClassTeacherAccess(userId, studentClass, section);
        return Map.of(
                "classTeacher", classTeacher,
                "anyAccess", classTeacher || service.hasAnyAccess(userId, studentClass, section)
        );
    }

    private Long requireUserId(HttpServletRequest request) {
        Long uid = UserContext.userId(request);
        if (uid == null) throw new UnauthorizedException("Authentication required");
        return uid;
    }
}