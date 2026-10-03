package com.successacademy.staffservice.service;

import com.successacademy.staffservice.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StaffService {

    StaffResponse createStaff(StaffRequest request, Long actorUserId);

    StaffResponse updateStaff(Long id, StaffRequest request, Long actorUserId);

    StaffResponse updateStatus(Long id, StaffStatusUpdateRequest request, Long actorUserId);

    StaffResponse provisionAccess(Long id, StaffProvisionRequest request, Long actorUserId);

    StaffResponse disableAccess(Long id, Long actorUserId);

    StaffResponse getStaffById(Long id);

    StaffResponse getStaffByUserId(Long userId);

    StaffResponse getStaffByCode(String staffCode);

    List<StaffResponse> searchStaff(
            String keyword,
            String category,
            Long departmentId,
            Long designationId,
            String employmentType,
            String status,
            String accessStatus
    );

    String uploadPhoto(Long id, MultipartFile file, Long actorUserId);

    StaffStatsResponse getStats();

    String getNextStaffCode();
}
