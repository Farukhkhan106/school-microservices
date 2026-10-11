package com.successacademy.authservice.service.user;

import com.successacademy.authservice.dto.rbac.AssignUserRoleRequest;
import com.successacademy.authservice.dto.rbac.EffectiveAuthorizationResponse;
import com.successacademy.authservice.dto.rbac.UserRoleDto;
import com.successacademy.authservice.dto.user.*;
import org.springframework.data.domain.Page;

public interface UserService {

    Page<UserResponseDto> searchUsers(String tenantId, String query, String role, String status, String userType,
                                     int page, int size, String sortBy, String sortDir, Long actorId, String actorRole);

    UserResponseDto getUserById(Long userId, String tenantId, Long actorId, String actorRole);

    UserResponseDto createUser(CreateUserRequest request, String tenantId, Long actorId, String actorUsername, String actorRole);

    UserResponseDto updateUser(Long userId, UpdateUserRequest request, String tenantId, Long actorId, String actorUsername, String actorRole);

    UserResponseDto activateUser(Long userId, String tenantId, Long actorId, String actorUsername, String actorRole);

    UserResponseDto deactivateUser(Long userId, String tenantId, Long actorId, String actorUsername, String actorRole);

    UserResponseDto suspendUser(Long userId, String tenantId, Long actorId, String actorUsername, String actorRole);

    UserRoleDto assignRole(Long userId, AssignUserRoleRequest request, String tenantId, Long actorId, String actorUsername, String actorRole);

    void revokeRole(Long userId, Long roleId, String tenantId, Long actorId, String actorUsername, String actorRole);

    EffectiveAuthorizationResponse getEffectivePermissions(Long userId, String tenantId);

    InvitationResponseDto inviteUser(InviteUserRequest request, String tenantId, Long actorId, String actorUsername, String actorRole);

    InvitationResponseDto resendInvitation(Long userId, String tenantId, Long actorId, String actorUsername, String actorRole);

    void acceptInvitation(AcceptInvitationRequest request);

    void initiatePasswordReset(PasswordResetRequest request, String tenantId, Long actorId, String actorUsername, String actorRole);

    void confirmPasswordReset(ConfirmPasswordResetRequest request);

    void forcePasswordReset(Long userId, ForcePasswordResetRequest request, String tenantId, Long actorId, String actorUsername, String actorRole);
}
