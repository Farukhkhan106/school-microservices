package com.successacademy.staffservice.service;

import com.successacademy.staffservice.dto.ShiftAssignmentRequest;
import com.successacademy.staffservice.dto.ShiftAssignmentResponse;
import com.successacademy.staffservice.dto.ShiftRequest;
import com.successacademy.staffservice.dto.ShiftResponse;

import java.time.LocalDate;
import java.util.List;

public interface ShiftService {

    List<ShiftResponse> getAllShifts(boolean activeOnly);

    ShiftResponse getShiftById(Long id);

    ShiftResponse createShift(ShiftRequest request, Long actorUserId);

    ShiftResponse updateShift(Long id, ShiftRequest request, Long actorUserId);

    ShiftAssignmentResponse assignShiftToStaff(ShiftAssignmentRequest request, Long actorUserId);

    List<ShiftAssignmentResponse> getShiftAssignmentsForStaff(Long staffId);

    ShiftAssignmentResponse getCurrentShiftForStaff(Long staffId, LocalDate date);
}
