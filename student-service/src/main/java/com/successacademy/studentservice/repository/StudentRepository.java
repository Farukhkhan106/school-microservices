package com.successacademy.studentservice.repository;

import com.successacademy.studentservice.model.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByIdAndTenantId(Long id, String tenantId);

    List<Student> findByTenantId(String tenantId);

    List<Student> findByTenantIdAndStudentClassAndSectionIgnoreCase(String tenantId, String studentClass, String section);

    List<Student> findByTenantIdAndStudentClass(String tenantId, String studentClass);

    List<Student> findByStudentClassAndSectionIgnoreCase(String studentClass, String section);

    List<Student> findByStudentClass(String studentClass);

    long countByTenantIdAndStudentClassAndSection(String tenantId, String studentClass, String section);

    boolean existsByTenantIdAndAdmissionNoIgnoreCase(String tenantId, String admissionNo);

    boolean existsByAdmissionNoIgnoreCase(String admissionNo);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Student> findByTenantIdAndAdmissionNo(String tenantId, String admissionNo);

    Optional<Student> findByTenantIdAndAdmissionId(String tenantId, Long admissionId);

    @Query("""
        SELECT s FROM Student s
        WHERE s.tenantId = :tenantId
          AND (:studentClass IS NULL OR s.studentClass = :studentClass)
          AND (:section IS NULL OR LOWER(s.section) = LOWER(:section))
          AND (:status IS NULL OR LOWER(s.status) = LOWER(:status) OR LOWER(s.lifecycleStatus) = LOWER(:status))
          AND (:search IS NULL OR LOWER(s.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(s.admissionNo) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(s.rollNo) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<Student> searchStudents(
            @Param("tenantId") String tenantId,
            @Param("studentClass") String studentClass,
            @Param("section") String section,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable
    );
}
