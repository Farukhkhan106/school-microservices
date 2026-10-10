package com.successacademy.communicationservice.repository;

import com.successacademy.communicationservice.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByTenantIdAndUserIdOrderByCreatedAtDesc(String tenantId, Long userId);

    Page<Notification> findByTenantIdAndUserIdOrderByCreatedAtDesc(String tenantId, Long userId, Pageable pageable);

    long countByTenantIdAndUserIdAndReadFalse(String tenantId, Long userId);

    Optional<Notification> findByIdAndTenantIdAndUserId(Long id, String tenantId, Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :now WHERE n.tenantId = :tenantId AND n.userId = :userId AND n.read = false")
    int markAllAsRead(@Param("tenantId") String tenantId, @Param("userId") Long userId, @Param("now") LocalDateTime now);
}
