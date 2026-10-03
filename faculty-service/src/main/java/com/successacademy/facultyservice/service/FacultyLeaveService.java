package com.successacademy.facultyservice.service;

import com.successacademy.facultyservice.dto.FacultyLeaveApprovalRequest;
import com.successacademy.facultyservice.dto.FacultyLeaveRequestDto;
import com.successacademy.facultyservice.dto.FacultyLeaveResponseDto;

import java.util.List;

public interface FacultyLeaveService {

    FacultyLeaveResponseDto applyLeave(FacultyLeaveRequestDto request, Long actorUserId);

    List<FacultyLeaveResponseDto> getLeavesByStatus(String status);

    List<FacultyLeaveResponseDto> getLeavesForFaculty(Long facultyId);

    FacultyLeaveResponseDto processLeaveApproval(Long leaveId, FacultyLeaveApprovalRequest request, Long actorUserId);
}
