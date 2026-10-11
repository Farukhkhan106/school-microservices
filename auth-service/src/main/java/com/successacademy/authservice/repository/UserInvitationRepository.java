package com.successacademy.authservice.repository;

import com.successacademy.authservice.model.entity.UserInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserInvitationRepository extends JpaRepository<UserInvitation, Long> {

    Optional<UserInvitation> findByTokenHash(String tokenHash);

    List<UserInvitation> findByUserIdAndStatus(Long userId, String status);

    List<UserInvitation> findByTenantId(String tenantId);
}
