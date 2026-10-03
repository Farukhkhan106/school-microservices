package com.successacademy.staffservice.service;

import com.successacademy.staffservice.dto.DesignationRequest;
import com.successacademy.staffservice.dto.DesignationResponse;

import java.util.List;

public interface DesignationService {

    List<DesignationResponse> getAllDesignations(Long departmentId, boolean activeOnly);

    DesignationResponse getDesignationById(Long id);

    DesignationResponse createDesignation(DesignationRequest request, Long actorUserId);

    DesignationResponse updateDesignation(Long id, DesignationRequest request, Long actorUserId);

    DesignationResponse updateDesignationStatus(Long id, String status, Long actorUserId);
}
