package com.successacademy.authservice.controller;

import com.successacademy.authservice.dto.config.CreateTenantRequest;
import com.successacademy.authservice.dto.config.TenantDto;
import com.successacademy.authservice.dto.config.UpdateTenantRequest;
import com.successacademy.authservice.security.SecurityContextUtil;
import com.successacademy.authservice.service.config.TenantManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantManagementService tenantService;
    private final SecurityContextUtil securityContextUtil;

    @GetMapping
    public ResponseEntity<List<TenantDto>> listTenants(HttpServletRequest request) {
        securityContextUtil.requireAdmin(request);
        return ResponseEntity.ok(tenantService.listTenants());
    }

    @GetMapping("/{tenantKey}")
    public ResponseEntity<TenantDto> getTenant(
            @PathVariable String tenantKey,
            HttpServletRequest request) {
        securityContextUtil.requireAdmin(request);
        return ResponseEntity.ok(tenantService.getTenant(tenantKey));
    }

    @PostMapping
    public ResponseEntity<TenantDto> createTenant(
            @Valid @RequestBody CreateTenantRequest req,
            HttpServletRequest request) {
        securityContextUtil.requireAdmin(request);
        String actor = securityContextUtil.getCurrentUsername(request);
        return ResponseEntity.ok(tenantService.createTenant(req, actor));
    }

    @PutMapping("/{tenantKey}")
    public ResponseEntity<TenantDto> updateTenant(
            @PathVariable String tenantKey,
            @RequestBody UpdateTenantRequest req,
            HttpServletRequest request) {
        securityContextUtil.requireAdmin(request);
        String actor = securityContextUtil.getCurrentUsername(request);
        return ResponseEntity.ok(tenantService.updateTenant(tenantKey, req, actor));
    }
}
