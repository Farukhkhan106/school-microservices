package com.successacademy.facultyservice.service;

import com.successacademy.facultyservice.dto.FacultyCalculateSalaryRequest;
import com.successacademy.facultyservice.dto.FacultyMonthlySalaryResponse;
import com.successacademy.facultyservice.dto.FacultyPaymentRequest;
import com.successacademy.facultyservice.dto.FacultySalaryPaymentResponse;

import java.util.List;

public interface FacultySalaryService {

    List<FacultyMonthlySalaryResponse> calculateMonthlySalaries(FacultyCalculateSalaryRequest request, Long actorUserId);

    List<FacultyMonthlySalaryResponse> getMonthlySalaries(Integer month, Integer year);

    FacultyMonthlySalaryResponse getMonthlySalaryById(Long id);

    FacultyMonthlySalaryResponse recordSalaryPayment(Long salaryId, FacultyPaymentRequest request, Long actorUserId);

    List<FacultySalaryPaymentResponse> getSalaryPayments(Long monthlySalaryId);

    List<FacultyMonthlySalaryResponse> approveAllPayroll(Integer month, Integer year, Long actorUserId);

    FacultyMonthlySalaryResponse approvePayroll(Long id, Long actorUserId);

    List<FacultyMonthlySalaryResponse> getSalaryHistory(Long facultyId);
}
