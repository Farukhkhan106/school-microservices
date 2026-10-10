package com.successacademy.academicservice.service;

import com.successacademy.academicservice.client.StudentServiceClient;
import com.successacademy.academicservice.dto.SubmissionCreateRequest;
import com.successacademy.academicservice.dto.SubmissionResponse;
import com.successacademy.academicservice.model.*;
import com.successacademy.academicservice.repository.AcademicActivityRepository;
import com.successacademy.academicservice.repository.ActivitySubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivitySubmissionService {

    private final ActivitySubmissionRepository submissionRepository;
    private final AcademicActivityRepository activityRepository;
    private final StudentServiceClient studentServiceClient;
    private final AuditLogService auditLogService;

    @Transactional
    public SubmissionResponse submitWork(
            Long activityId,
            Long studentId,
            SubmissionCreateRequest req,
            Long actorUserId,
            String actorRole
    ) {
        if (studentId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authenticated student identity.");
        }

        AcademicActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found with ID: " + activityId));

        if (activity.getStatus() != ActivityStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot submit to an activity that is not currently published.");
        }

        // Verify Student Enrollment in the activity's class and section
        StudentServiceClient.StudentInfoDto student = studentServiceClient.getStudentById(studentId);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student record not found.");
        }

        boolean classMatches = student.getStudentClass() != null &&
                student.getStudentClass().trim().equalsIgnoreCase(activity.getStudentClass().trim());
        boolean sectionMatches = student.getSection() != null &&
                student.getSection().trim().equalsIgnoreCase(activity.getSection().trim());

        if (!classMatches || !sectionMatches) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    String.format("Access denied: You belong to Class %s-%s, but this activity is assigned to Class %s-%s.",
                            student.getStudentClass(), student.getSection(), activity.getStudentClass(), activity.getSection())
            );
        }

        // Determine submission status based on Due Date
        LocalDate today = LocalDate.now();
        SubmissionStatus submissionStatus = today.isAfter(activity.getDueDate())
                ? SubmissionStatus.LATE
                : SubmissionStatus.SUBMITTED;

        String studentFullName = ((student.getFirstName() != null ? student.getFirstName() : "") + " " +
                (student.getLastName() != null ? student.getLastName() : "")).trim();

        boolean hasText = req.getSubmissionText() != null && !req.getSubmissionText().trim().isEmpty();
        boolean hasAttachment = req.getAttachmentUrl() != null && !req.getAttachmentUrl().trim().isEmpty();
        if (!hasText && !hasAttachment) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Submission must contain either text or an attached file.");
        }

        String textContent = hasText ? req.getSubmissionText().trim() : "";

        ActivitySubmission submission = submissionRepository.findByActivityIdAndStudentId(activityId, studentId)
                .orElse(null);

        if (submission == null) {
            submission = ActivitySubmission.builder()
                    .activityId(activityId)
                    .studentId(studentId)
                    .studentName(studentFullName)
                    .studentAdmissionNo(student.getAdmissionNo())
                    .submissionText(textContent)
                    .attachmentUrl(req.getAttachmentUrl())
                    .attachmentName(req.getAttachmentName())
                    .attachmentSize(req.getAttachmentSize())
                    .attachmentType(req.getAttachmentType())
                    .submittedAt(LocalDateTime.now())
                    .status(submissionStatus)
                    .attemptNumber(1)
                    .build();
        } else {
            if (submission.getStatus() == SubmissionStatus.EVALUATED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot resubmit work that has already been evaluated.");
            }
            submission.setSubmissionText(textContent);
            if (hasAttachment) {
                submission.setAttachmentUrl(req.getAttachmentUrl());
                submission.setAttachmentName(req.getAttachmentName());
                submission.setAttachmentSize(req.getAttachmentSize());
                submission.setAttachmentType(req.getAttachmentType());
            }
            submission.setSubmittedAt(LocalDateTime.now());
            submission.setStatus(submissionStatus);
            submission.setAttemptNumber(submission.getAttemptNumber() != null ? submission.getAttemptNumber() + 1 : 1);
        }

        ActivitySubmission saved = submissionRepository.save(submission);

        auditLogService.log(
                actorUserId,
                actorRole,
                "SUBMISSION_CREATED",
                "ActivitySubmission",
                saved.getId(),
                null,
                "Attempt " + saved.getAttemptNumber() + " (" + saved.getStatus() + ")",
                "Student " + studentId + " submitted work for Activity " + activityId
        );

        return mapToResponse(saved);
    }

    public SubmissionResponse getMySubmission(Long activityId, Long studentId) {
        if (studentId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authenticated student identity.");
        }

        AcademicActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found with ID: " + activityId));

        // Verify Student Enrollment
        StudentServiceClient.StudentInfoDto student = studentServiceClient.getStudentById(studentId);
        if (student == null ||
                !activity.getStudentClass().equalsIgnoreCase(student.getStudentClass()) ||
                !activity.getSection().equalsIgnoreCase(student.getSection())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Activity does not belong to your class.");
        }

        return submissionRepository.findByActivityIdAndStudentId(activityId, studentId)
                .map(this::mapToResponse)
                .orElse(null);
    }

    public List<SubmissionResponse> getSubmissionsByActivity(
            Long activityId,
            Long teacherId,
            boolean isAdmin
    ) {
        AcademicActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found with ID: " + activityId));

        if (!isAdmin) {
            if (teacherId == null || !teacherId.equals(activity.getTeacherId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You can only view submissions for your own activities.");
            }
        }

        return submissionRepository.findByActivityId(activityId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public SubmissionResponse mapToResponse(ActivitySubmission s) {
        return SubmissionResponse.builder()
                .id(s.getId())
                .activityId(s.getActivityId())
                .studentId(s.getStudentId())
                .studentName(s.getStudentName())
                .studentAdmissionNo(s.getStudentAdmissionNo())
                .submissionText(s.getSubmissionText())
                .attachmentUrl(s.getAttachmentUrl())
                .attachmentName(s.getAttachmentName())
                .attachmentSize(s.getAttachmentSize())
                .attachmentType(s.getAttachmentType())
                .submittedAt(s.getSubmittedAt())
                .status(s.getStatus())
                .attemptNumber(s.getAttemptNumber())
                .teacherFeedback(s.getTeacherFeedback())
                .evaluatedAt(s.getEvaluatedAt())
                .evaluatedBy(s.getEvaluatedBy())
                .build();
    }
}
