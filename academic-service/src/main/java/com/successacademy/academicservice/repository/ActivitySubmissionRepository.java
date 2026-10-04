package com.successacademy.academicservice.repository;

import com.successacademy.academicservice.model.ActivitySubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivitySubmissionRepository extends JpaRepository<ActivitySubmission, Long> {

    Optional<ActivitySubmission> findByActivityIdAndStudentId(Long activityId, Long studentId);

    List<ActivitySubmission> findByActivityId(Long activityId);

    List<ActivitySubmission> findByStudentId(Long studentId);

    long countByActivityId(Long activityId);
}
