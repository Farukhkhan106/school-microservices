package com.successacademy.studentservice.service;

import com.successacademy.studentservice.dto.StudentProfileResponse;
import com.successacademy.studentservice.dto.StudentRequest;
import com.successacademy.studentservice.dto.StudentResponse;

import java.util.List;

public interface StudentService {

    StudentResponse addStudent(StudentRequest request);

    StudentResponse updateStudent(Long id, StudentRequest request);

    StudentResponse getStudentById(Long id);

    StudentProfileResponse getMyProfile(Long id);


    List<StudentResponse> getAllStudents();

    void deleteStudent(Long id);

    String uploadPhoto(Long id, org.springframework.web.multipart.MultipartFile file);

    String uploadGeneralPhoto(org.springframework.web.multipart.MultipartFile file);
}
