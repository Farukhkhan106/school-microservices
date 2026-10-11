package com.successacademy.authservice.repository;

import com.successacademy.authservice.model.entity.AppEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppEnvironmentRepository extends JpaRepository<AppEnvironment, Long> {
    Optional<AppEnvironment> findByEnvironmentKey(String environmentKey);
    boolean existsByEnvironmentKey(String environmentKey);
}
