package com.successacademy.authservice.controller;

import com.successacademy.authservice.dto.dashboard.EffectiveDashboardResponse;
import com.successacademy.authservice.dto.dashboard.NavigationItemDto;
import com.successacademy.authservice.repository.UserRepository;
import com.successacademy.authservice.security.SecurityContextUtil;
import com.successacademy.authservice.security.TenantContext;
import com.successacademy.authservice.service.dashboard.DynamicDashboardService;
import com.successacademy.authservice.service.dashboard.DynamicNavigationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping({"/dashboard", "/auth-service/dashboard"})
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DynamicDashboardService dashboardService;
    private final DynamicNavigationService navigationService;
    private final SecurityContextUtil securityContextUtil;
    private final UserRepository userRepository;

    /**
     * Resolves effective dashboard for current tenant and user:
     * - School branding & Academic session
     * - Hierarchical navigation (pruned according to permissions & features)
     * - Authorized dashboard widgets with backend-scoped data
     * - Authorized quick actions
     * - Real dynamic badges
     */
    @GetMapping("/effective")
    public ResponseEntity<EffectiveDashboardResponse> getEffectiveDashboard(
            @RequestParam(value = "tenant", required = false) String tenantParam,
            @RequestParam(value = "previewRole", required = false) String previewRole,
            HttpServletRequest request) {

        String tenant = resolveTenant(tenantParam, request);
        Long userId = resolveUserId(request);
        String username = securityContextUtil.getCurrentUsername(request);
        String role = securityContextUtil.getCurrentRole(request);

        // Preview role simulation is restricted to administrators
        String safePreviewRole = null;
        if (previewRole != null && !previewRole.isBlank()) {
            if (!isAdminUser(role)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only administrators can simulate preview roles.");
            }
            safePreviewRole = previewRole.trim().toUpperCase();
        }

        EffectiveDashboardResponse response = dashboardService.resolveDashboard(
                userId, username, tenant, role, safePreviewRole);

        return ResponseEntity.ok(response);
    }

    /**
     * Resolves dynamic, hierarchical navigation for the current user.
     */
    @GetMapping("/navigation")
    public ResponseEntity<List<NavigationItemDto>> getNavigation(
            @RequestParam(value = "tenant", required = false) String tenantParam,
            @RequestParam(value = "previewRole", required = false) String previewRole,
            HttpServletRequest request) {

        String tenant = resolveTenant(tenantParam, request);
        Long userId = resolveUserId(request);
        String role = securityContextUtil.getCurrentRole(request);

        String safePreviewRole = null;
        if (previewRole != null && !previewRole.isBlank()) {
            if (!isAdminUser(role)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only administrators can simulate preview roles.");
            }
            safePreviewRole = previewRole.trim().toUpperCase();
        }

        List<NavigationItemDto> navigation = navigationService.resolveNavigation(
                userId, tenant, role, safePreviewRole);

        return ResponseEntity.ok(navigation);
    }

    /**
     * Preview mode endpoint for administrators to simulate UI across different roles.
     */
    @GetMapping("/preview")
    public ResponseEntity<EffectiveDashboardResponse> previewRoleDashboard(
            @RequestParam("role") String roleToPreview,
            @RequestParam(value = "tenant", required = false) String tenantParam,
            HttpServletRequest request) {

        String currentRole = securityContextUtil.getCurrentRole(request);
        if (!isAdminUser(currentRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Preview mode is only accessible to administrators.");
        }

        String tenant = resolveTenant(tenantParam, request);
        Long userId = resolveUserId(request);
        String username = securityContextUtil.getCurrentUsername(request);

        EffectiveDashboardResponse response = dashboardService.resolveDashboard(
                userId, username, tenant, currentRole, roleToPreview.trim().toUpperCase());

        return ResponseEntity.ok(response);
    }

    private String resolveTenant(String tenantParam, HttpServletRequest request) {
        String currentRole = securityContextUtil.getCurrentRole(request);
        // Only admins can override tenant param; regular users strictly bound to authenticated tenant
        if (tenantParam != null && !tenantParam.isBlank() && isAdminUser(currentRole)) {
            return tenantParam.trim();
        }
        String headerTenant = securityContextUtil.getTenantId(request);
        return (headerTenant != null && !headerTenant.isBlank()) ? headerTenant : TenantContext.getTenantId();
    }

    private Long resolveUserId(HttpServletRequest request) {
        Long uid = securityContextUtil.getCurrentUserId(request);
        if (uid != null) {
            return uid;
        }
        String uname = securityContextUtil.getCurrentUsername(request);
        if (uname != null && !uname.isBlank()) {
            return userRepository.findByUsername(uname).map(u -> u.getId()).orElse(null);
        }
        return null;
    }

    private boolean isAdminUser(String role) {
        if (role == null) return false;
        String upper = role.toUpperCase();
        return upper.contains("ADMIN") || upper.equals("SUPER_ADMIN") || upper.equals("PLATFORM_ADMIN");
    }
}
