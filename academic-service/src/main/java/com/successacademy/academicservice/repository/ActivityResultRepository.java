package com.successacademy.academicservice.repository;

import com.successacademy.academicservice.model.ActivityResult;
import com.successacademy.academicservice.model.ResultStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityResultRepository extends JpaRepository<ActivityResult, Long> {

    Optional<ActivityResult> findByActivityIdAndStudentId(Long activityId, Long studentId);

    List<ActivityResult> findByActivityId(Long activityId);

    List<ActivityResult> findByActivityIdAndStatus(Long activityId, ResultStatus status);

    List<ActivityResult> findByStudentIdAndStatus(Long studentId, ResultStatus status);

    List<ActivityResult> findByStudentId(Long studentId);
}
