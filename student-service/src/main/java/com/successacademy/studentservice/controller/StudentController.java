package com.successacademy.studentservice.controller;

import com.successacademy.studentservice.client.FacultyServiceClient;
import com.successacademy.studentservice.dto.*;
import com.successacademy.studentservice.model.Student;
import com.successacademy.studentservice.security.SecurityContextUtil;
import com.successacademy.studentservice.security.TenantContext;
import com.successacademy.studentservice.security.UserContext;
import com.successacademy.studentservice.service.GuardianService;
import com.successacademy.studentservice.service.StudentLifecycleService;
import com.successacademy.studentservice.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final FacultyServiceClient facultyServiceClient;
    private final StudentLifecycleService lifecycleService;
    private final GuardianService guardianService;
    private final SecurityContextUtil securityContextUtil;

    @PostMapping("/add")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse addStudent(@RequestBody StudentRequest request, HttpServletRequest httpRequest) {
        enforceAdmin(httpRequest);
        return studentService.addStudent(request);
    }

    @PutMapping("/{id}")
    public StudentResponse updateStudent(
            @PathVariable Long id,
            @RequestBody StudentRequest request,
            HttpServletRequest httpRequest) {
        enforceAdmin(httpRequest);
        return studentService.updateStudent(id, request);
    }

    @GetMapping("/class")
    public List<StudentResponse> getStudentsByClass(
            @RequestParam String studentClass,
            @RequestParam(required = false) String section) {
        return studentService.getStudentsByClassAndSection(studentClass, section);
    }

    @GetMapping("/assigned")
    public List<StudentResponse> getAssignedStudents(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Teacher-Id", required = false) Long teacherId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        return studentService.getAssignedStudents(userId, teacherId, role);
    }

    @GetMapping("/profile/{id:[0-9]+}")
    public StudentProfileResponse getMyProfile(@PathVariable Long id, HttpServletRequest request) {
        enforceAccess(id, request);
        return studentService.getMyProfile(id);
    }

    @GetMapping("/{id:[0-9]+}/detailed-profile")
    public StudentDetailedProfileResponse getDetailedProfile(@PathVariable Long id, HttpServletRequest request) {
        enforceAccess(id, request);
        return studentService.getDetailedProfile(id);
    }

    @GetMapping("/{id:[0-9]+}")
    public StudentResponse getStudentById(@PathVariable Long id, HttpServletRequest request) {
        enforceAccess(id, request);
        return studentService.getStudentById(id);
    }

    @GetMapping
    public List<StudentResponse> getAllStudents(HttpServletRequest request) {
        UserContext ctx = UserContext.fromRequest(request);
        if ("TEACHER".equalsIgnoreCase(ctx.getRole())) {
            return studentService.getAssignedStudents(ctx.getUserId(), ctx.getRole());
        }
        return studentService.getAllStudents();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStudent(@PathVariable Long id, HttpServletRequest httpRequest) {
        enforceAdmin(httpRequest);
        studentService.deleteStudent(id);
    }

    @PostMapping("/{id}/photo")
    public java.util.Map<String, String> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            HttpServletRequest request) {
        enforceAccess(id, request);
        String url = studentService.uploadPhoto(id, file);
        return java.util.Map.of("photoUrl", url);
    }

    @PostMapping("/upload-photo")
    public java.util.Map<String, String> uploadGeneralPhoto(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            HttpServletRequest httpRequest) {
        enforceAdmin(httpRequest);
        String url = studentService.uploadGeneralPhoto(file);
        return java.util.Map.of("photoUrl", url);
    }

    @PutMapping("/{id}/toggle")
    public StudentResponse toggleStatus(@PathVariable Long id, HttpServletRequest httpRequest) {
        enforceAdmin(httpRequest);
        return studentService.toggleStatus(id);
    }

    @RequestMapping(value = "/{id}/academic-placement", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    public StudentResponse updateAcademicPlacement(
            @PathVariable Long id,
            @RequestParam(required = false) String studentClass,
            @RequestParam(required = false) String section,
            @RequestParam(required = false) String rollNo,
            @RequestParam(required = false) String status,
            HttpServletRequest request
    ) {
        enforceAccess(id, request);
        return studentService.updateAcademicPlacement(id, studentClass, section, rollNo, status);
    }

    // ─────────────────────────────────────────────────────────────
    //  STUDENT LIFECYCLE MANAGEMENT (Phase 8)
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/{id}/transfer")
    public StudentResponse transferStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentLifecycleActionRequest req,
            HttpServletRequest httpRequest) {
        enforceAdmin(httpRequest);
        req.setAction("TRANSFER");
        String actor = securityContextUtil.getCurrentUsername(httpRequest);
        Student updated = lifecycleService.executeLifecycleAction(id, req, actor);
        return studentService.getStudentById(updated.getId());
    }

    @PostMapping("/{id}/withdraw")
    public StudentResponse withdrawStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentLifecycleActionRequest req,
            HttpServletRequest httpRequest) {
        enforceAdmin(httpRequest);
        req.setAction("WITHDRAW");
        String actor = securityContextUtil.getCurrentUsername(httpRequest);
        Student updated = lifecycleService.executeLifecycleAction(id, req, actor);
        return studentService.getStudentById(updated.getId());
    }

    @PostMapping("/{id}/graduate")
    public StudentResponse graduateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentLifecycleActionRequest req,
            HttpServletRequest httpRequest) {
        enforceAdmin(httpRequest);
        req.setAction("GRADUATE");
        String actor = securityContextUtil.getCurrentUsername(httpRequest);
        Student updated = lifecycleService.executeLifecycleAction(id, req, actor);
        return studentService.getStudentById(updated.getId());
    }

    @PostMapping("/{id}/lifecycle")
    public StudentResponse executeLifecycle(
            @PathVariable Long id,
            @Valid @RequestBody StudentLifecycleActionRequest req,
            HttpServletRequest httpRequest) {
        enforceAdmin(httpRequest);
        String actor = securityContextUtil.getCurrentUsername(httpRequest);
        Student updated = lifecycleService.executeLifecycleAction(id, req, actor);
        return studentService.getStudentById(updated.getId());
    }

    @GetMapping("/{id}/lifecycle-history")
    public List<StudentLifecycleLogDto> getLifecycleHistory(@PathVariable Long id, HttpServletRequest httpRequest) {
        enforceAccess(id, httpRequest);
        return lifecycleService.getLifecycleHistory(id);
    }

    private void enforceAdmin(HttpServletRequest request) {
        if (securityContextUtil.isTrustedInternalCall(request)) {
            return;
        }
        UserContext ctx = UserContext.fromRequest(request);
        if (ctx.getRole() == null) {
            return; // Internal service call
        }
        if (!securityContextUtil.isAdmin(request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Administrator role required.");
        }
    }

    private void enforceAccess(Long studentId, HttpServletRequest request) {
        if (securityContextUtil.isTrustedInternalCall(request)) {
            return;
        }
        UserContext ctx = UserContext.fromRequest(request);
        if (ctx.getRole() == null) {
            return; // Internal service call
        }
        if (securityContextUtil.isAdmin(request)) {
            return; // Admin can access any student
        }
        if ("STUDENT".equalsIgnoreCase(ctx.getRole())) {
            if (ctx.getStudentId() == null || !ctx.getStudentId().equals(studentId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You can only view your own student record");
            }
            return;
        }
        if ("GUARDIAN".equalsIgnoreCase(ctx.getRole())) {
            Long guardianId = securityContextUtil.getCurrentGuardianId(request);
            if (guardianId == null || !guardianService.isGuardianLinkedToStudent(guardianId, studentId, TenantContext.getTenantId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Guardian is not linked to this student");
            }
            return;
        }
        if ("TEACHER".equalsIgnoreCase(ctx.getRole())) {
            StudentResponse target = studentService.getStudentById(studentId);
            if (target == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
            }
            List<String> allowed = facultyServiceClient.getAllowedClasses(ctx.getUserId(), ctx.getTeacherId());
            String studentClassSection = target.getStudentClass() + "-" + (target.getSection() != null ? target.getSection().trim() : "");
            boolean hasAccess = allowed != null && allowed.stream().anyMatch(a -> a.equalsIgnoreCase(studentClassSection));
            if (!hasAccess) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Student does not belong to your assigned classes");
            }
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }
}
