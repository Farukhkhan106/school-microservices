package com.successacademy.academicservice.repository;

import com.successacademy.academicservice.model.AcademicActivity;
import com.successacademy.academicservice.model.ActivityStatus;
import com.successacademy.academicservice.model.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademicActivityRepository extends JpaRepository<AcademicActivity, Long>, JpaSpecificationExecutor<AcademicActivity> {

    List<AcademicActivity> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    List<AcademicActivity> findByTeacherIdAndStatusOrderByCreatedAtDesc(Long teacherId, ActivityStatus status);

    List<AcademicActivity> findByStudentClassAndSectionAndStatusOrderByDueDateDesc(
            String studentClass, String section, ActivityStatus status
    );

    List<AcademicActivity> findByStudentClassAndSectionOrderByCreatedAtDesc(
            String studentClass, String section
    );

    @Query("SELECT a FROM AcademicActivity a WHERE a.teacherId = :teacherId " +
           "AND (:studentClass IS NULL OR a.studentClass = :studentClass) " +
           "AND (:section IS NULL OR a.section = :section) " +
           "AND (:subject IS NULL OR a.subject = :subject) " +
           "AND (:type IS NULL OR a.activityType = :type) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "ORDER BY a.createdAt DESC")
    List<AcademicActivity> findTeacherActivitiesFiltered(
            @Param("teacherId") Long teacherId,
            @Param("studentClass") String studentClass,
            @Param("section") String section,
            @Param("subject") String subject,
            @Param("type") ActivityType type,
            @Param("status") ActivityStatus status
    );

    @Query("SELECT a FROM AcademicActivity a WHERE a.studentClass = :studentClass " +
           "AND a.section = :section " +
           "AND a.status = :status " +
           "AND (:subject IS NULL OR a.subject = :subject) " +
           "AND (:type IS NULL OR a.activityType = :type) " +
           "ORDER BY a.dueDate DESC")
    List<AcademicActivity> findStudentActivitiesFiltered(
            @Param("studentClass") String studentClass,
            @Param("section") String section,
            @Param("status") ActivityStatus status,
            @Param("subject") String subject,
            @Param("type") ActivityType type
    );
}
