package com.successacademy.facultyservice.controller;

import com.successacademy.facultyservice.dto.FacultyCalculateSalaryRequest;
import com.successacademy.facultyservice.dto.FacultyMonthlySalaryResponse;
import com.successacademy.facultyservice.dto.FacultyPaymentRequest;
import com.successacademy.facultyservice.dto.FacultySalaryPaymentResponse;
import com.successacademy.facultyservice.security.SecurityContextUtil;
import com.successacademy.facultyservice.service.FacultySalaryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/faculty/salary")
@RequiredArgsConstructor
@Slf4j
public class FacultySalaryController {

    private final FacultySalaryService salaryService;
    private final SecurityContextUtil securityUtil;

    @PostMapping("/monthly/calculate")
    public ResponseEntity<List<FacultyMonthlySalaryResponse>> calculateMonthlySalaries(
            @RequestBody(required = false) FacultyCalculateSalaryRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        FacultyCalculateSalaryRequest r = (request != null) ? request : new FacultyCalculateSalaryRequest();
        List<FacultyMonthlySalaryResponse> res = salaryService.calculateMonthlySalaries(r, actorUserId);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<FacultyMonthlySalaryResponse>> getMonthlySalaries(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        List<FacultyMonthlySalaryResponse> res = salaryService.getMonthlySalaries(month, year);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/monthly/{id}")
    public ResponseEntity<FacultyMonthlySalaryResponse> getMonthlySalaryById(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        FacultyMonthlySalaryResponse res = salaryService.getMonthlySalaryById(id);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/monthly/{id}/record-payment")
    public ResponseEntity<FacultyMonthlySalaryResponse> recordPayment(
            @PathVariable Long id,
            @RequestBody FacultyPaymentRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        FacultyMonthlySalaryResponse res = salaryService.recordSalaryPayment(id, request, actorUserId);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/monthly/{id}/payments")
    public ResponseEntity<List<FacultySalaryPaymentResponse>> getSalaryPayments(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        List<FacultySalaryPaymentResponse> res = salaryService.getSalaryPayments(id);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/monthly/{id}/approve")
    public ResponseEntity<FacultyMonthlySalaryResponse> approvePayroll(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        FacultyMonthlySalaryResponse res = salaryService.approvePayroll(id, actorUserId);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/monthly/approve-all")
    public ResponseEntity<List<FacultyMonthlySalaryResponse>> approveAllPayroll(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        List<FacultyMonthlySalaryResponse> res = salaryService.approveAllPayroll(month, year, actorUserId);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/history/{facultyId}")
    public ResponseEntity<List<FacultyMonthlySalaryResponse>> getSalaryHistory(
            @PathVariable Long facultyId,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        List<FacultyMonthlySalaryResponse> res = salaryService.getSalaryHistory(facultyId);
        return ResponseEntity.ok(res);
    }
}
