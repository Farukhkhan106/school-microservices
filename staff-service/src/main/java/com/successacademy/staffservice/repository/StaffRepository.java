package com.successacademy.staffservice.repository;

import com.successacademy.staffservice.model.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long>, JpaSpecificationExecutor<Staff> {

    Optional<Staff> findByStaffCode(String staffCode);

    Optional<Staff> findByUserId(Long userId);

    boolean existsByStaffCode(String staffCode);

    Optional<Staff> findTopByOrderByIdDesc();

    long countByDepartmentId(Long departmentId);

    long countByDesignationId(Long designationId);

    long countByStatus(String status);

    long countBySystemAccessStatus(String systemAccessStatus);

    @Query("SELECT s FROM Staff s WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(s.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(s.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(s.staffCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(s.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:category IS NULL OR :category = '' OR :category = 'ALL' OR LOWER(s.category) = LOWER(:category)) AND " +
           "(:departmentId IS NULL OR s.departmentId = :departmentId) AND " +
           "(:designationId IS NULL OR s.designationId = :designationId) AND " +
           "(:employmentType IS NULL OR :employmentType = '' OR s.employmentType = :employmentType) AND " +
           "(:status IS NULL OR :status = '' OR s.status = :status) AND " +
           "(:accessStatus IS NULL OR :accessStatus = '' OR s.systemAccessStatus = :accessStatus) " +
           "ORDER BY s.id ASC")
    List<Staff> searchStaff(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("departmentId") Long departmentId,
            @Param("designationId") Long designationId,
            @Param("employmentType") String employmentType,
            @Param("status") String status,
            @Param("accessStatus") String accessStatus
    );
}
