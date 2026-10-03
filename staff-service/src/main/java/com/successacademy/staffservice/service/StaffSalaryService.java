package com.successacademy.staffservice.service;

import com.successacademy.staffservice.dto.CalculateSalaryRequest;
import com.successacademy.staffservice.dto.StaffMonthlySalaryResponse;
import com.successacademy.staffservice.dto.StaffSalaryConfigRequest;
import com.successacademy.staffservice.dto.StaffSalaryConfigResponse;

import java.util.List;

public interface StaffSalaryService {

    StaffSalaryConfigResponse getSalaryConfig(Long staffId);

    StaffSalaryConfigResponse saveSalaryConfig(Long staffId, StaffSalaryConfigRequest request, Long actorUserId);

    List<StaffMonthlySalaryResponse> getMonthlySalaries(Integer month, Integer year);

    List<StaffMonthlySalaryResponse> calculateMonthlySalaries(CalculateSalaryRequest request, Long actorUserId);

    StaffMonthlySalaryResponse markSalaryPaid(Long monthlySalaryId, Long actorUserId);

    StaffMonthlySalaryResponse recordSalaryPayment(Long monthlySalaryId, com.successacademy.staffservice.dto.StaffPaymentRequest request, Long actorUserId);

    StaffMonthlySalaryResponse approvePayroll(Long monthlySalaryId, Long actorUserId);

    List<StaffMonthlySalaryResponse> approveAllPayroll(Integer month, Integer year, Long actorUserId);

    StaffMonthlySalaryResponse unlockPayroll(Long monthlySalaryId, String reason, Long actorUserId);

    List<com.successacademy.staffservice.dto.StaffSalaryPaymentResponse> getSalaryPayments(Long monthlySalaryId);

    List<StaffMonthlySalaryResponse> getSalaryHistory(Long staffId);
}
