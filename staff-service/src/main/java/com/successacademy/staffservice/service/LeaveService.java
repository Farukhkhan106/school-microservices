package com.successacademy.staffservice.service;

import com.successacademy.staffservice.dto.LeaveApprovalRequest;
import com.successacademy.staffservice.dto.LeaveRequestDto;
import com.successacademy.staffservice.dto.LeaveResponseDto;

import java.util.List;

public interface LeaveService {

    LeaveResponseDto applyLeave(LeaveRequestDto request, Long staffId, Long actorUserId);

    List<LeaveResponseDto> getLeavesForStaff(Long staffId);

    List<LeaveResponseDto> getLeavesByStatus(String status);

    LeaveResponseDto processLeaveApproval(Long leaveId, LeaveApprovalRequest request, Long actorUserId);
}
