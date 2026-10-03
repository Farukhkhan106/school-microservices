package com.successacademy.staffservice.controller;

import com.successacademy.staffservice.dto.*;
import com.successacademy.staffservice.security.SecurityContextUtil;
import com.successacademy.staffservice.service.StaffSalaryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/salary")
@RequiredArgsConstructor
@Slf4j
public class StaffSalaryController {

    private final StaffSalaryService salaryService;
    private final SecurityContextUtil securityUtil;

    @GetMapping("/config/{staffId}")
    public ResponseEntity<StaffSalaryConfigResponse> getSalaryConfig(
            @PathVariable Long staffId,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        StaffSalaryConfigResponse res = salaryService.getSalaryConfig(staffId);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/config/{staffId}")
    public ResponseEntity<StaffSalaryConfigResponse> saveSalaryConfig(
            @PathVariable Long staffId,
            @Valid @RequestBody StaffSalaryConfigRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        StaffSalaryConfigResponse res = salaryService.saveSalaryConfig(staffId, request, actorUserId);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<StaffMonthlySalaryResponse>> getMonthlySalaries(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        List<StaffMonthlySalaryResponse> res = salaryService.getMonthlySalaries(month, year);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/monthly/calculate")
    public ResponseEntity<List<StaffMonthlySalaryResponse>> calculateMonthlySalaries(
            @RequestBody(required = false) CalculateSalaryRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        CalculateSalaryRequest r = (request != null) ? request : new CalculateSalaryRequest();
        List<StaffMonthlySalaryResponse> res = salaryService.calculateMonthlySalaries(r, actorUserId);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/monthly/{id}/pay")
    public ResponseEntity<StaffMonthlySalaryResponse> markSalaryPaid(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        StaffMonthlySalaryResponse res = salaryService.markSalaryPaid(id, actorUserId);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/monthly/{id}/record-payment")
    public ResponseEntity<StaffMonthlySalaryResponse> recordSalaryPayment(
            @PathVariable Long id,
            @Valid @RequestBody StaffPaymentRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        StaffMonthlySalaryResponse res = salaryService.recordSalaryPayment(id, request, actorUserId);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/monthly/{id}/approve")
    public ResponseEntity<StaffMonthlySalaryResponse> approvePayroll(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        StaffMonthlySalaryResponse res = salaryService.approvePayroll(id, actorUserId);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/monthly/approve-all")
    public ResponseEntity<List<StaffMonthlySalaryResponse>> approveAllPayroll(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        List<StaffMonthlySalaryResponse> res = salaryService.approveAllPayroll(month, year, actorUserId);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/monthly/{id}/unlock")
    public ResponseEntity<StaffMonthlySalaryResponse> unlockPayroll(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "Unlocked by admin";
        StaffMonthlySalaryResponse res = salaryService.unlockPayroll(id, reason, actorUserId);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/monthly/{id}/payments")
    public ResponseEntity<List<StaffSalaryPaymentResponse>> getSalaryPayments(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        List<StaffSalaryPaymentResponse> res = salaryService.getSalaryPayments(id);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/history/{staffId}")
    public ResponseEntity<List<StaffMonthlySalaryResponse>> getSalaryHistory(
            @PathVariable Long staffId,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        List<StaffMonthlySalaryResponse> res = salaryService.getSalaryHistory(staffId);
        return ResponseEntity.ok(res);
    }
}
