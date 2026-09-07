package com.successacademy.studentservice.controller;

import com.successacademy.studentservice.dto.StudentProfileResponse;
import com.successacademy.studentservice.dto.StudentRequest;
import com.successacademy.studentservice.dto.StudentResponse;
import com.successacademy.studentservice.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

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
    public StudentProfileResponse getMyProfile(@PathVariable Long id) {
        return studentService.getMyProfile(id);
    }

    @GetMapping("/{id}")
    public StudentResponse getStudentById(@PathVariable Long id) {
        return studentService.getStudentById(id);
    }

    @GetMapping
    public List<StudentResponse> getAllStudents() {
        return studentService.getAllStudents();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
    }

    @PostMapping("/{id}/photo")
    public java.util.Map<String, String> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String url = studentService.uploadPhoto(id, file);
        return java.util.Map.of("photoUrl", url);
    }

    @PostMapping("/upload-photo")
    public java.util.Map<String, String> uploadGeneralPhoto(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String url = studentService.uploadGeneralPhoto(file);
        return java.util.Map.of("photoUrl", url);
    }
}
