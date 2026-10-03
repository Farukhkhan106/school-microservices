package com.successacademy.staffservice.service;

import com.successacademy.staffservice.dto.DepartmentRequest;
import com.successacademy.staffservice.dto.DepartmentResponse;

import java.util.List;

public interface DepartmentService {

    List<DepartmentResponse> getAllDepartments(boolean activeOnly);

    DepartmentResponse getDepartmentById(Long id);

    DepartmentResponse createDepartment(DepartmentRequest request, Long actorUserId);

    DepartmentResponse updateDepartment(Long id, DepartmentRequest request, Long actorUserId);

    DepartmentResponse updateDepartmentStatus(Long id, String status, Long actorUserId);
}
