package com.successacademy.authservice.repository;

import com.successacademy.authservice.model.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByPermissionKey(String permissionKey);
    List<Permission> findByCategory(String category);
    List<Permission> findByIsProtectedFalse();
}
