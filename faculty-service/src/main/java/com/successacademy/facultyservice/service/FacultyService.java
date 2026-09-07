package com.successacademy.facultyservice.service;

import com.successacademy.facultyservice.dto.FacultyRequest;
import com.successacademy.facultyservice.dto.FacultyResponse;

import java.util.List;

public interface FacultyService {

    FacultyResponse addFaculty(FacultyRequest request);

    FacultyResponse updateFaculty(Long id, FacultyRequest request);

    void deleteFaculty(Long id);

    List<FacultyResponse> getAllFaculty();

    List<FacultyResponse> getActiveFaculty();    // public website

    FacultyResponse getFacultyById(Long id);

    FacultyResponse getFacultyByUserId(Long userId);

    List<FacultyResponse> searchByName(String keyword);

    List<FacultyResponse> filterBySubject(String subject);

    String uploadPhoto(Long id, org.springframework.web.multipart.MultipartFile file);

    String uploadGeneralPhoto(org.springframework.web.multipart.MultipartFile file);
}
