package com.successacademy.staffservice.repository;

import com.successacademy.staffservice.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByCodeIgnoreCase(String code);

    Optional<Department> findByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    List<Department> findByStatusIgnoreCase(String status);
}
