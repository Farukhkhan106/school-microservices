package com.successacademy.studentservice.controller;

import com.successacademy.studentservice.client.FacultyServiceClient;
import com.successacademy.studentservice.dto.StudentProfileResponse;
import com.successacademy.studentservice.dto.StudentRequest;
import com.successacademy.studentservice.dto.StudentResponse;
import com.successacademy.studentservice.security.UserContext;
import com.successacademy.studentservice.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
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

    @PostMapping("/add")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse addStudent(@RequestBody StudentRequest request) {
        return studentService.addStudent(request);
    }

    @PutMapping("/{id}")
    public StudentResponse updateStudent(
            @PathVariable Long id,
            @RequestBody StudentRequest request) {
        return studentService.updateStudent(id, request);
    }

    @GetMapping("/profile/{id}")
    public StudentProfileResponse getMyProfile(@PathVariable Long id, HttpServletRequest request) {
        enforceAccess(id, request);
        return studentService.getMyProfile(id);
    }

    @GetMapping("/{id}")
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

    @GetMapping("/assigned")
    public List<StudentResponse> getAssignedStudents(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        return studentService.getAssignedStudents(userId, role);
    }

    @GetMapping("/class")
    public List<StudentResponse> getStudentsByClass(
            @RequestParam String studentClass,
            @RequestParam(required = false) String section) {
        return studentService.getStudentsByClassAndSection(studentClass, section);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStudent(@PathVariable Long id) {
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
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String url = studentService.uploadGeneralPhoto(file);
        return java.util.Map.of("photoUrl", url);
    }

    @PutMapping("/{id}/toggle")
    public StudentResponse toggleStatus(@PathVariable Long id) {
        return studentService.toggleStatus(id);
    }

    private void enforceAccess(Long studentId, HttpServletRequest request) {
        UserContext ctx = UserContext.fromRequest(request);
        if (ctx.getRole() == null) {
            return; // Internal service call without gateway headers
        }
        if ("ADMIN".equalsIgnoreCase(ctx.getRole())) {
            return; // Admin can access any student
        }
        if ("STUDENT".equalsIgnoreCase(ctx.getRole())) {
            if (ctx.getStudentId() == null || !ctx.getStudentId().equals(studentId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You can only view your own student record");
            }
            return;
        }
        if ("TEACHER".equalsIgnoreCase(ctx.getRole())) {
            StudentResponse target = studentService.getStudentById(studentId);
            if (target == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
            }
            List<String> allowed = facultyServiceClient.getAllowedClasses(ctx.getUserId());
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
