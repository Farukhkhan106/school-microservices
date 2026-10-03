package com.successacademy.staffservice.repository;

import com.successacademy.staffservice.model.Designation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DesignationRepository extends JpaRepository<Designation, Long> {

    List<Designation> findByDepartmentId(Long departmentId);

    List<Designation> findByDepartmentIdAndStatusIgnoreCase(Long departmentId, String status);

    Optional<Designation> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCaseAndDepartmentId(String name, Long departmentId);

    List<Designation> findByStatusIgnoreCase(String status);
}
