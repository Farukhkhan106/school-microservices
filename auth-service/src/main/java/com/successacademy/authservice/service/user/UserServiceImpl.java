package com.successacademy.authservice.service.user;

import com.successacademy.authservice.dto.rbac.AssignUserRoleRequest;
import com.successacademy.authservice.dto.rbac.EffectiveAuthorizationResponse;
import com.successacademy.authservice.dto.rbac.UserRoleDto;
import com.successacademy.authservice.dto.user.*;
import com.successacademy.authservice.model.User;
import com.successacademy.authservice.model.entity.PasswordResetToken;
import com.successacademy.authservice.model.entity.Role;
import com.successacademy.authservice.model.entity.UserInvitation;
import com.successacademy.authservice.repository.*;
import com.successacademy.authservice.service.rbac.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserInvitationRepository invitationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RbacService rbacService;
    private final UserAuditService auditService;
    private final PasswordEncoder passwordEncoder;

    // =========================================================================
    // SEARCH & RETRIEVAL
    // =========================================================================

    @Override
    public Page<UserResponseDto> searchUsers(String tenantId, String query, String role, String status, String userType,
                                            int page, int size, String sortBy, String sortDir, Long actorId, String actorRole) {
        String cleanTenant = resolveTenant(tenantId, actorRole);
        Sort sort = Sort.by("asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC,
                (sortBy != null && !sortBy.isBlank()) ? sortBy : "id");
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)), sort);

        String cleanQuery = (query != null && !query.trim().isBlank()) ? query.trim() : null;
        String cleanRole = (role != null && !role.trim().isBlank() && !"ALL".equalsIgnoreCase(role)) ? role.trim().toUpperCase() : null;
        String cleanStatus = (status != null && !status.trim().isBlank() && !"ALL".equalsIgnoreCase(status)) ? status.trim().toUpperCase() : null;
        String cleanUserType = (userType != null && !userType.trim().isBlank() && !"ALL".equalsIgnoreCase(userType)) ? userType.trim().toUpperCase() : null;

        Page<User> usersPage = userRepository.searchUsers(cleanTenant, cleanQuery, cleanStatus, cleanUserType, cleanRole, pageable);

        return usersPage.map(u -> toResponseDto(u, cleanTenant, false));
    }

    @Override
    public UserResponseDto getUserById(Long userId, String tenantId, Long actorId, String actorRole) {
        String cleanTenant = resolveTenant(tenantId, actorRole);
        User user = findUserWithTenantCheck(userId, cleanTenant, actorRole);
        return toResponseDto(user, cleanTenant, true);
    }

    // =========================================================================
    // USER CREATION & PROFILE UPDATES
    // =========================================================================

    @Override
    @Transactional
    public UserResponseDto createUser(CreateUserRequest req, String tenantId, Long actorId, String actorUsername, String actorRole) {
        String cleanTenant = resolveTenant(tenantId, actorRole);
        String cleanUsername = req.getUsername() != null ? req.getUsername().trim().toLowerCase() : "";

        if (cleanUsername.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty.");
        }

        if (userRepository.existsByUsernameAndTenantId(cleanUsername, cleanTenant)
                || userRepository.findByUsername(cleanUsername).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username '" + cleanUsername + "' is already taken.");
        }

        if (req.getEmail() != null && !req.getEmail().trim().isBlank()) {
            String cleanEmail = req.getEmail().trim().toLowerCase();
            if (userRepository.existsByEmailAndTenantId(cleanEmail, cleanTenant)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email '" + cleanEmail + "' is already registered in this tenant.");
            }
        }

        // Validate Role assignment permissions if a role is requested
        Role targetRole = null;
        if (req.getRoleId() != null) {
            targetRole = roleRepository.findById(req.getRoleId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found with id: " + req.getRoleId()));
        } else if (req.getRoleCode() != null && !req.getRoleCode().isBlank()) {
            String rc = req.getRoleCode().trim().toUpperCase();
            targetRole = roleRepository.findByRoleCodeAndTenantId(rc, cleanTenant)
                    .or(() -> roleRepository.findByRoleCodeAndTenantIdIsNull(rc))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found with code: " + rc));
        }

        if (targetRole != null) {
            validateRoleAssignmentPrivileges(targetRole, actorRole);
        }

        String initialStatus = req.isSendInvitation() ? "INVITED" : "ACTIVE";
        String rawPassword = (req.getPassword() != null && !req.getPassword().isBlank())
                ? req.getPassword()
                : UUID.randomUUID().toString().substring(0, 12);

        String userType = (req.getUserType() != null && !req.getUserType().isBlank())
                ? req.getUserType().trim().toUpperCase()
                : (targetRole != null ? targetRole.getRoleCode() : "STAFF");

        User newUser = User.builder()
                .username(cleanUsername)
                .email(req.getEmail() != null ? req.getEmail().trim().toLowerCase() : null)
                .phone(req.getPhone() != null ? req.getPhone().trim() : null)
                .password(passwordEncoder.encode(rawPassword))
                .firstName(req.getFirstName() != null ? req.getFirstName().trim() : null)
                .lastName(req.getLastName() != null ? req.getLastName().trim() : null)
                .displayName(req.getDisplayName() != null ? req.getDisplayName().trim() : null)
                .userType(userType)
                .role(targetRole != null ? targetRole.getRoleCode() : "STAFF")
                .tenantId(cleanTenant)
                .status(initialStatus)
                .studentId(req.getStudentId())
                .teacherId(req.getTeacherId())
                .staffId(req.getStaffId())
                .mustChangePassword(req.isMustChangePassword())
                .createdBy(actorUsername)
                .updatedBy(actorUsername)
                .build();

        User saved = userRepository.save(newUser);

        // Bind role in user_roles
        if (targetRole != null) {
            AssignUserRoleRequest assignReq = AssignUserRoleRequest.builder()
                    .roleId(targetRole.getId())
                    .isPrimary(true)
                    .build();
            rbacService.assignUserRole(saved.getId(), assignReq, cleanTenant, actorId, actorUsername, actorRole);
        }

        auditService.log(cleanTenant, actorId, actorUsername, actorRole, saved.getId(), saved.getUsername(),
                "USER_CREATED", "Created user " + saved.getUsername() + " [status: " + initialStatus + ", type: " + userType + "]");

        return toResponseDto(saved, cleanTenant, true);
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(Long userId, UpdateUserRequest req, String tenantId, Long actorId, String actorUsername, String actorRole) {
        String cleanTenant = resolveTenant(tenantId, actorRole);
        User user = findUserWithTenantCheck(userId, cleanTenant, actorRole);

        if (req.getEmail() != null) user.setEmail(req.getEmail().trim().toLowerCase());
        if (req.getPhone() != null) user.setPhone(req.getPhone().trim());
        if (req.getFirstName() != null) user.setFirstName(req.getFirstName().trim());
        if (req.getLastName() != null) user.setLastName(req.getLastName().trim());
        if (req.getDisplayName() != null) user.setDisplayName(req.getDisplayName().trim());
        if (req.getUserType() != null && !req.getUserType().isBlank()) {
            // Cannot elevate to PLATFORM_ADMIN
            if ("PLATFORM_ADMIN".equalsIgnoreCase(req.getUserType()) && !isPlatformAdmin(actorRole)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot set user type to PLATFORM_ADMIN.");
            }
            user.setUserType(req.getUserType().trim().toUpperCase());
        }

        if (req.getStudentId() != null) user.setStudentId(req.getStudentId());
        if (req.getTeacherId() != null) user.setTeacherId(req.getTeacherId());
        if (req.getStaffId() != null) user.setStaffId(req.getStaffId());

        user.setUpdatedBy(actorUsername);
        User saved = userRepository.save(user);

        auditService.log(cleanTenant, actorId, actorUsername, actorRole, saved.getId(), saved.getUsername(),
                "USER_UPDATED", "Updated profile attributes for " + saved.getUsername());

        return toResponseDto(saved, cleanTenant, true);
    }

    // =========================================================================
    // LIFECYCLE STATE TRANSITIONS
    // =========================================================================

    @Override
    @Transactional
    public UserResponseDto activateUser(Long userId, String tenantId, Long actorId, String actorUsername, String actorRole) {
        String cleanTenant = resolveTenant(tenantId, actorRole);
        User user = findUserWithTenantCheck(userId, cleanTenant, actorRole);

        user.setStatus("ACTIVE");
        user.setUpdatedBy(actorUsername);
        User saved = userRepository.save(user);

        rbacService.invalidateCache(cleanTenant, userId);
        auditService.log(cleanTenant, actorId, actorUsername, actorRole, saved.getId(), saved.getUsername(),
                "USER_ACTIVATED", "Account activated");

        return toResponseDto(saved, cleanTenant, true);
    }

    @Override
    @Transactional
    public UserResponseDto deactivateUser(Long userId, String tenantId, Long actorId, String actorUsername, String actorRole) {
        String cleanTenant = resolveTenant(tenantId, actorRole);
        User user = findUserWithTenantCheck(userId, cleanTenant, actorRole);

        // Step 10: Last Tenant Admin Protection
        checkLastTenantAdminProtection(user, cleanTenant, "deactivate");

        // Self-protection
        if (userId.equals(actorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Self-deactivation prohibited.");
        }

        user.setStatus("INACTIVE");
        user.setUpdatedBy(actorUsername);
        User saved = userRepository.save(user);

        rbacService.invalidateCache(cleanTenant, userId);
        auditService.log(cleanTenant, actorId, actorUsername, actorRole, saved.getId(), saved.getUsername(),
                "USER_DEACTIVATED", "Account deactivated");

        return toResponseDto(saved, cleanTenant, true);
    }

    @Override
    @Transactional
    public UserResponseDto suspendUser(Long userId, String tenantId, Long actorId, String actorUsername, String actorRole) {
        String cleanTenant = resolveTenant(tenantId, actorRole);
        User user = findUserWithTenantCheck(userId, cleanTenant, actorRole);

        // Step 10: Last Tenant Admin Protection
        checkLastTenantAdminProtection(user, cleanTenant, "suspend");

        // Self-protection
        if (userId.equals(actorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Self-suspension prohibited.");
        }

        user.setStatus("SUSPENDED");
        user.setUpdatedBy(actorUsername);
        User saved = userRepository.save(user);

        rbacService.invalidateCache(cleanTenant, userId);
        auditService.log(cleanTenant, actorId, actorUsername, actorRole, saved.getId(), saved.getUsername(),
                "USER_SUSPENDED", "Account suspended");

        return toResponseDto(saved, cleanTenant, true);
    }

    // =========================================================================
    // ROLE ASSIGNMENT & REVOCATION
    // =========================================================================

    @Override
    @Transactional
    public UserRoleDto assignRole(Long userId, AssignUserRoleRequest req, String tenantId, Long actorId, String actorUsername, String actorRole) {
        String cleanTenant = resolveTenant(tenantId, actorRole);
        User targetUser = findUserWithTenantCheck(userId, cleanTenant, actorRole);

        Role role = roleRepository.findById(req.getRoleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found with id: " + req.getRoleId()));

        validateRoleAssignmentPrivileges(role, actorRole);

        UserRoleDto result = rbacService.assignUserRole(userId, req, cleanTenant, actorId, actorUsername, actorRole);

        // Keep legacy role column aligned if primary
        if (req.isPrimary()) {
            targetUser.setRole(role.getRoleCode());
            userRepository.save(targetUser);
        }

        return result;
    }

    @Override
    @Transactional
    public void revokeRole(Long userId, Long roleId, String tenantId, Long actorId, String actorUsername, String actorRole) {
        String cleanTenant = resolveTenant(tenantId, actorRole);
        User targetUser = findUserWithTenantCheck(userId, cleanTenant, actorRole);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found with id: " + roleId));

        // Last Tenant Admin Protection when revoking TENANT_ADMIN role
        if ("TENANT_ADMIN".equalsIgnoreCase(role.getRoleCode())) {
            checkLastTenantAdminProtection(targetUser, cleanTenant, "revoke Tenant Administrator role from");
        }

        rbacService.revokeUserRole(userId, roleId, cleanTenant, actorId, actorUsername, actorRole);
    }

    @Override
    public EffectiveAuthorizationResponse getEffectivePermissions(Long userId, String tenantId) {
        String cleanTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId.trim() : "default";
        return rbacService.getEffectiveAuthorization(userId, cleanTenant);
    }

    // =========================================================================
    // INVITATIONS LIFECYCLE
    // =========================================================================

    @Override
    @Transactional
    public InvitationResponseDto inviteUser(InviteUserRequest req, String tenantId, Long actorId, String actorUsername, String actorRole) {
        String cleanTenant = resolveTenant(tenantId, actorRole);
        String cleanEmail = (req.getEmail() != null) ? req.getEmail().trim().toLowerCase() : "";

        if (cleanEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required to invite user.");
        }

        String username = (req.getUsername() != null && !req.getUsername().isBlank())
                ? req.getUsername().trim().toLowerCase()
                : cleanEmail.split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 4);

        CreateUserRequest createReq = CreateUserRequest.builder()
                .username(username)
                .email(cleanEmail)
                .phone(req.getPhone())
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .userType(req.getUserType())
                .roleId(req.getRoleId())
                .roleCode(req.getRoleCode())
                .studentId(req.getStudentId())
                .teacherId(req.getTeacherId())
                .staffId(req.getStaffId())
                .sendInvitation(true)
                .mustChangePassword(true)
                .build();

        UserResponseDto createdUser = createUser(createReq, cleanTenant, actorId, actorUsername, actorRole);
        User user = userRepository.findById(createdUser.getId()).orElseThrow();

        // Generate secure one-time invitation token
        String rawToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        String tokenHash = hashToken(rawToken);

        UserInvitation invitation = UserInvitation.builder()
                .tenantId(cleanTenant)
                .user(user)
                .email(cleanEmail)
                .roleId(req.getRoleId())
                .tokenHash(tokenHash)
                .status("PENDING")
                .expiresAt(LocalDateTime.now().plusHours(48))
                .createdBy(actorUsername)
                .build();

        UserInvitation savedInv = invitationRepository.save(invitation);

        auditService.log(cleanTenant, actorId, actorUsername, actorRole, user.getId(), user.getUsername(),
                "INVITATION_SENT", "Invitation sent to " + cleanEmail);

        return InvitationResponseDto.builder()
                .id(savedInv.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .email(cleanEmail)
                .status(savedInv.getStatus())
                .expiresAt(savedInv.getExpiresAt())
                .rawToken(rawToken) // Returned once to caller
                .createdAt(savedInv.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public InvitationResponseDto resendInvitation(Long userId, String tenantId, Long actorId, String actorUsername, String actorRole) {
        String cleanTenant = resolveTenant(tenantId, actorRole);
        User user = findUserWithTenantCheck(userId, cleanTenant, actorRole);

        // Invalidate old pending invitations
        List<UserInvitation> existing = invitationRepository.findByUserIdAndStatus(userId, "PENDING");
        for (UserInvitation inv : existing) {
            inv.setStatus("REVOKED");
            invitationRepository.save(inv);
        }

        String rawToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        String tokenHash = hashToken(rawToken);

        UserInvitation newInv = UserInvitation.builder()
                .tenantId(cleanTenant)
                .user(user)
                .email(user.getEmail() != null ? user.getEmail() : "user@school.internal")
                .tokenHash(tokenHash)
                .status("PENDING")
                .expiresAt(LocalDateTime.now().plusHours(48))
                .createdBy(actorUsername)
                .build();

        UserInvitation saved = invitationRepository.save(newInv);

        auditService.log(cleanTenant, actorId, actorUsername, actorRole, user.getId(), user.getUsername(),
                "INVITATION_RESENT", "Invitation resent to user " + user.getUsername());

        return InvitationResponseDto.builder()
                .id(saved.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .email(saved.getEmail())
                .status(saved.getStatus())
                .expiresAt(saved.getExpiresAt())
                .rawToken(rawToken)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void acceptInvitation(AcceptInvitationRequest req) {
        if (req.getToken() == null || req.getToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation token is required.");
        }
        if (req.getNewPassword() == null || req.getNewPassword().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters.");
        }

        String tokenHash = hashToken(req.getToken().trim());
        UserInvitation invitation = invitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired invitation token."));

        if (!"PENDING".equalsIgnoreCase(invitation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation token has already been used or revoked.");
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus("EXPIRED");
            invitationRepository.save(invitation);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation token has expired.");
        }

        User user = invitation.getUser();
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setStatus("ACTIVE");
        user.setMustChangePassword(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        invitation.setStatus("ACCEPTED");
        invitation.setUsedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

        rbacService.invalidateCache(invitation.getTenantId(), user.getId());

        auditService.log(invitation.getTenantId(), user.getId(), user.getUsername(), user.getRole(),
                user.getId(), user.getUsername(), "INVITATION_ACCEPTED", "Invitation accepted and account activated.");
    }

    // =========================================================================
    // PASSWORD RESET
    // =========================================================================

    @Override
    @Transactional
    public void initiatePasswordReset(PasswordResetRequest req, String tenantId, Long actorId, String actorUsername, String actorRole) {
        String cleanTenant = resolveTenant(tenantId, actorRole);
        if (req.getUsernameOrEmail() == null || req.getUsernameOrEmail().isBlank()) {
            return; // Generic silent return to prevent enumeration
        }

        String search = req.getUsernameOrEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByUsernameAndTenantId(search, cleanTenant)
                .or(() -> userRepository.findAll().stream()
                        .filter(u -> cleanTenant.equals(u.getTenantId()) && search.equalsIgnoreCase(u.getEmail()))
                        .findFirst());

        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existent target: {} in tenant {}", search, cleanTenant);
            return; // Safe generic response
        }

        User user = userOpt.get();

        // Invalidate old tokens
        List<PasswordResetToken> existing = passwordResetTokenRepository.findByUserIdAndStatus(user.getId(), "PENDING");
        for (PasswordResetToken t : existing) {
            t.setStatus("EXPIRED");
            passwordResetTokenRepository.save(t);
        }

        String rawToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        String tokenHash = hashToken(rawToken);

        PasswordResetToken token = PasswordResetToken.builder()
                .tenantId(cleanTenant)
                .user(user)
                .tokenHash(tokenHash)
                .status("PENDING")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        passwordResetTokenRepository.save(token);

        auditService.log(cleanTenant, actorId, actorUsername, actorRole, user.getId(), user.getUsername(),
                "PASSWORD_RESET_INITIATED", "Password reset requested for " + user.getUsername());
    }

    @Override
    @Transactional
    public void confirmPasswordReset(ConfirmPasswordResetRequest req) {
        if (req.getToken() == null || req.getToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password reset token is required.");
        }
        if (req.getNewPassword() == null || req.getNewPassword().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters.");
        }

        String tokenHash = hashToken(req.getToken().trim());
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired password reset token."));

        if (!"PENDING".equalsIgnoreCase(token.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password reset token has already been used.");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            token.setStatus("EXPIRED");
            passwordResetTokenRepository.save(token);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password reset token has expired.");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setMustChangePassword(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        token.setStatus("USED");
        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);

        rbacService.invalidateCache(token.getTenantId(), user.getId());

        auditService.log(token.getTenantId(), user.getId(), user.getUsername(), user.getRole(),
                user.getId(), user.getUsername(), "PASSWORD_RESET_COMPLETED", "Password successfully reset via token.");
    }

    @Override
    @Transactional
    public void forcePasswordReset(Long userId, ForcePasswordResetRequest req, String tenantId, Long actorId, String actorUsername, String actorRole) {
        String cleanTenant = resolveTenant(tenantId, actorRole);
        User user = findUserWithTenantCheck(userId, cleanTenant, actorRole);

        if (req.getNewPassword() == null || req.getNewPassword().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be at least 6 characters.");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setMustChangePassword(req.isMustChangePassword());
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy(actorUsername);
        userRepository.save(user);

        rbacService.invalidateCache(cleanTenant, userId);

        auditService.log(cleanTenant, actorId, actorUsername, actorRole, user.getId(), user.getUsername(),
                "PASSWORD_FORCE_RESET", "Password was force-reset by administrator " + actorUsername);
    }

    // =========================================================================
    // SAFETY & SECURITY INVARIANTS
    // =========================================================================

    private void checkLastTenantAdminProtection(User targetUser, String tenantId, String actionDescription) {
        // Only applies if the user is currently ACTIVE
        if (!"ACTIVE".equalsIgnoreCase(targetUser.getStatus())) {
            return;
        }

        // Check if user has TENANT_ADMIN role
        List<UserRoleDto> roles = rbacService.getUserRoles(targetUser.getId(), tenantId);
        boolean isTenantAdmin = roles.stream().anyMatch(r -> "TENANT_ADMIN".equalsIgnoreCase(r.getRoleCode()))
                || "ADMIN".equalsIgnoreCase(targetUser.getRole())
                || "TENANT_ADMIN".equalsIgnoreCase(targetUser.getRole());

        if (isTenantAdmin) {
            long otherAdminsCount = userRepository.countOtherActiveUsersWithRole(tenantId, "TENANT_ADMIN", targetUser.getId());
            if (otherAdminsCount == 0) {
                log.warn("Blocked attempt to {} last active Tenant Administrator id {} in tenant {}", actionDescription, targetUser.getId(), tenantId);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Safety invariant violation: Cannot " + actionDescription + " user '" + targetUser.getUsername() +
                                "'. This user is the last active Tenant Administrator for tenant '" + tenantId +
                                "'. A tenant cannot be left without an administrator.");
            }
        }
    }

    private void validateRoleAssignmentPrivileges(Role targetRole, String actorRole) {
        boolean isPlatformAdmin = isPlatformAdmin(actorRole);

        // 1. Protected roles (like PLATFORM_ADMIN) require platform privileges
        if (targetRole.isProtected() && !isPlatformAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: You cannot assign protected role '" + targetRole.getRoleCode() + "'.");
        }

        // 2. Admin Delegate restrictions
        if ("ADMIN_DELEGATE".equalsIgnoreCase(actorRole)) {
            if ("TENANT_ADMIN".equalsIgnoreCase(targetRole.getRoleCode())
                    || "PLATFORM_ADMIN".equalsIgnoreCase(targetRole.getRoleCode())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Admin Delegate cannot assign Tenant Administrator or Platform Administrator roles.");
            }
        }
    }

    private User findUserWithTenantCheck(Long userId, String tenantId, String actorRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + userId));

        if (!isPlatformAdmin(actorRole) && user.getTenantId() != null && !user.getTenantId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + userId);
        }

        return user;
    }

    private String resolveTenant(String tenantId, String actorRole) {
        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId.trim();
        }
        return "default";
    }

    private boolean isPlatformAdmin(String actorRole) {
        return "PLATFORM_ADMIN".equalsIgnoreCase(actorRole) || "SUPER_ADMIN".equalsIgnoreCase(actorRole);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private UserResponseDto toResponseDto(User u, String tenantId, boolean includePermissions) {
        List<UserRoleDto> roles = rbacService.getUserRoles(u.getId(), tenantId);
        List<String> effectivePermissions = Collections.emptyList();
        Map<String, String> effectiveScopes = Collections.emptyMap();

        if (includePermissions) {
            try {
                EffectiveAuthorizationResponse authz = rbacService.getEffectiveAuthorization(u.getId(), tenantId);
                if (authz != null) {
                    effectivePermissions = authz.getPermissions();
                    effectiveScopes = authz.getPermissionScopes();
                }
            } catch (Exception ex) {
                log.debug("Could not resolve effective permissions for user {}: {}", u.getId(), ex.getMessage());
            }
        }

        return UserResponseDto.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .phone(u.getPhone())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .displayName(u.getDisplayName() != null ? u.getDisplayName() : u.getUsername())
                .role(u.getRole())
                .userType(u.getUserType())
                .status(u.getStatus())
                .tenantId(u.getTenantId())
                .studentId(u.getStudentId())
                .teacherId(u.getTeacherId())
                .staffId(u.getStaffId())
                .mustChangePassword(u.isMustChangePassword())
                .lastLoginAt(u.getLastLoginAt())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .createdBy(u.getCreatedBy())
                .updatedBy(u.getUpdatedBy())
                .roles(roles)
                .effectivePermissions(effectivePermissions)
                .effectiveScopes(effectiveScopes)
                .build();
    }
}
