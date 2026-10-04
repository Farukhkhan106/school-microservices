package com.successacademy.academicservice.service;

import com.successacademy.academicservice.client.StudentServiceClient;
import com.successacademy.academicservice.dto.*;
import com.successacademy.academicservice.model.*;
import com.successacademy.academicservice.repository.AcademicActivityRepository;
import com.successacademy.academicservice.repository.ActivityResultRepository;
import com.successacademy.academicservice.repository.ActivitySubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityResultService {

    private final ActivityResultRepository resultRepository;
    private final ActivitySubmissionRepository submissionRepository;
    private final AcademicActivityRepository activityRepository;
    private final StudentServiceClient studentServiceClient;
    private final GradingService gradingService;
    private final AuditLogService auditLogService;

    public List<ActivityRosterStudentResponse> getActivityRoster(
            Long activityId,
            Long teacherId,
            boolean isAdmin
    ) {
        AcademicActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found with ID: " + activityId));

        if (!isAdmin) {
            if (teacherId == null || !teacherId.equals(activity.getTeacherId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You can only view results for your own activities.");
            }
        }

        // 1. Fetch real students from student-service
        List<StudentServiceClient.StudentInfoDto> students = studentServiceClient.getStudentsByClassAndSection(
                activity.getStudentClass(), activity.getSection()
        );

        // 2. Fetch existing submissions & results
        Map<Long, ActivitySubmission> submissionMap = submissionRepository.findByActivityId(activityId).stream()
                .collect(Collectors.toMap(ActivitySubmission::getStudentId, s -> s, (s1, s2) -> s1));

        Map<Long, ActivityResult> resultMap = resultRepository.findByActivityId(activityId).stream()
                .collect(Collectors.toMap(ActivityResult::getStudentId, r -> r, (r1, r2) -> r1));

        return students.stream().map(st -> {
            ActivitySubmission sub = submissionMap.get(st.getId());
            ActivityResult res = resultMap.get(st.getId());

            String fullName = ((st.getFirstName() != null ? st.getFirstName() : "") + " " +
                    (st.getLastName() != null ? st.getLastName() : "")).trim();

            return ActivityRosterStudentResponse.builder()
                    .studentId(st.getId())
                    .admissionNo(st.getAdmissionNo())
                    .studentName(fullName)
                    .studentClass(st.getStudentClass())
                    .section(st.getSection())
                    .rollNo(st.getRollNo())
                    .submission(sub != null ? mapToSubmissionResponse(sub) : null)
                    .result(res != null ? mapToResultResponse(res) : null)
                    .build();
        }).toList();
    }

    @Transactional
    public List<ActivityResultResponse> saveResultsBatch(
            Long activityId,
            BatchActivityResultRequest req,
            Long teacherId,
            Long actorUserId,
            String actorRole,
            boolean isAdmin
    ) {
        AcademicActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found with ID: " + activityId));

        if (!isAdmin) {
            if (teacherId == null || !teacherId.equals(activity.getTeacherId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You can only enter marks for your own activities.");
            }
        }

        BigDecimal maxMarks = activity.getMaxMarks();
        List<ActivityResult> savedResults = new ArrayList<>();

        for (ActivityResultEntryRequest entry : req.getEntries()) {
            boolean isAbsent = Boolean.TRUE.equals(entry.getIsAbsent());
            BigDecimal obt = entry.getObtainedMarks();

            if (!isAbsent) {
                if (obt == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Obtained marks are required when student is present.");
                }
                if (obt.compareTo(BigDecimal.ZERO) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Obtained marks cannot be negative.");
                }
                if (obt.compareTo(maxMarks) > 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            String.format("Obtained marks (%.2f) cannot exceed maximum marks (%.2f).", obt, maxMarks));
                }
            } else {
                obt = null;
            }

            BigDecimal percentage = null;
            String grade = null;
            if (!isAbsent && obt != null && maxMarks.compareTo(BigDecimal.ZERO) > 0) {
                percentage = obt.divide(maxMarks, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);

                GradingRule rule = gradingService.calculateGrade(obt, maxMarks, null);
                if (rule != null) {
                    grade = rule.getGrade();
                }
            }

            ActivityResult result = resultRepository.findByActivityIdAndStudentId(activityId, entry.getStudentId())
                    .orElse(null);

            if (result == null) {
                StudentServiceClient.StudentInfoDto student = studentServiceClient.getStudentById(entry.getStudentId());
                String fullName = student != null
                        ? ((student.getFirstName() != null ? student.getFirstName() : "") + " " + (student.getLastName() != null ? student.getLastName() : "")).trim()
                        : "Student #" + entry.getStudentId();
                String admNo = student != null ? student.getAdmissionNo() : "";

                result = ActivityResult.builder()
                        .activityId(activityId)
                        .studentId(entry.getStudentId())
                        .studentName(fullName)
                        .studentAdmissionNo(admNo)
                        .maxMarks(maxMarks)
                        .obtainedMarks(obt)
                        .percentage(percentage)
                        .isAbsent(isAbsent)
                        .grade(grade)
                        .teacherFeedback(entry.getTeacherFeedback())
                        .status(ResultStatus.DRAFT)
                        .evaluatedBy(actorUserId)
                        .evaluatedAt(LocalDateTime.now())
                        .build();
            } else {
                result.setMaxMarks(maxMarks);
                result.setObtainedMarks(obt);
                result.setPercentage(percentage);
                result.setAbsent(isAbsent);
                result.setGrade(grade);
                result.setTeacherFeedback(entry.getTeacherFeedback());
                result.setEvaluatedBy(actorUserId);
                result.setEvaluatedAt(LocalDateTime.now());
            }

            savedResults.add(resultRepository.save(result));
        }

        auditLogService.log(
                actorUserId,
                actorRole,
                "RESULT_SAVED",
                "AcademicActivity",
                activityId,
                null,
                "Saved marks for " + savedResults.size() + " students",
                "Teacher saved draft results for Activity " + activityId
        );

        return savedResults.stream().map(this::mapToResultResponse).toList();
    }

    @Transactional
    public List<ActivityResultResponse> finalizeResults(
            Long activityId,
            Long teacherId,
            Long actorUserId,
            String actorRole,
            boolean isAdmin
    ) {
        AcademicActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found with ID: " + activityId));

        if (!isAdmin) {
            if (teacherId == null || !teacherId.equals(activity.getTeacherId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You can only finalize results for your own activities.");
            }
        }

        List<ActivityResult> results = resultRepository.findByActivityId(activityId);
        for (ActivityResult r : results) {
            r.setStatus(ResultStatus.FINALIZED);
        }
        List<ActivityResult> saved = resultRepository.saveAll(results);

        auditLogService.log(
                actorUserId,
                actorRole,
                "RESULT_FINALIZED",
                "AcademicActivity",
                activityId,
                "DRAFT",
                "FINALIZED",
                "Finalized " + saved.size() + " activity results"
        );

        return saved.stream().map(this::mapToResultResponse).toList();
    }

    @Transactional
    public List<ActivityResultResponse> publishResults(
            Long activityId,
            Long teacherId,
            Long actorUserId,
            String actorRole,
            boolean isAdmin
    ) {
        AcademicActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found with ID: " + activityId));

        if (!isAdmin) {
            if (teacherId == null || !teacherId.equals(activity.getTeacherId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You can only publish results for your own activities.");
            }
        }

        List<ActivityResult> results = resultRepository.findByActivityId(activityId);
        for (ActivityResult r : results) {
            r.setStatus(ResultStatus.PUBLISHED);
        }
        List<ActivityResult> saved = resultRepository.saveAll(results);

        auditLogService.log(
                actorUserId,
                actorRole,
                "RESULT_PUBLISHED",
                "AcademicActivity",
                activityId,
                "FINALIZED",
                "PUBLISHED",
                "Published " + saved.size() + " activity results for student visibility"
        );

        return saved.stream().map(this::mapToResultResponse).toList();
    }

    public ActivityResultResponse getMyResult(Long activityId, Long studentId) {
        if (studentId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authenticated student identity.");
        }

        ActivityResult result = resultRepository.findByActivityIdAndStudentId(activityId, studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No result record found for this activity."));

        // Crucial security rule: Students can ONLY see PUBLISHED results
        if (result.getStatus() != ResultStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Results for this activity have not been published yet.");
        }

        return mapToResultResponse(result);
    }

    public List<ActivityResultResponse> getStudentPublishedResults(Long studentId) {
        if (studentId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authenticated student identity.");
        }
        return resultRepository.findByStudentIdAndStatus(studentId, ResultStatus.PUBLISHED).stream()
                .map(this::mapToResultResponse)
                .toList();
    }

    private ActivityResultResponse mapToResultResponse(ActivityResult r) {
        return ActivityResultResponse.builder()
                .id(r.getId())
                .activityId(r.getActivityId())
                .studentId(r.getStudentId())
                .studentName(r.getStudentName())
                .studentAdmissionNo(r.getStudentAdmissionNo())
                .obtainedMarks(r.getObtainedMarks())
                .maxMarks(r.getMaxMarks())
                .percentage(r.getPercentage())
                .isAbsent(r.isAbsent())
                .grade(r.getGrade())
                .teacherFeedback(r.getTeacherFeedback())
                .status(r.getStatus())
                .evaluatedBy(r.getEvaluatedBy())
                .evaluatedAt(r.getEvaluatedAt())
                .build();
    }

    private SubmissionResponse mapToSubmissionResponse(ActivitySubmission s) {
        return SubmissionResponse.builder()
                .id(s.getId())
                .activityId(s.getActivityId())
                .studentId(s.getStudentId())
                .studentName(s.getStudentName())
                .studentAdmissionNo(s.getStudentAdmissionNo())
                .submissionText(s.getSubmissionText())
                .submittedAt(s.getSubmittedAt())
                .status(s.getStatus())
                .attemptNumber(s.getAttemptNumber())
                .teacherFeedback(s.getTeacherFeedback())
                .evaluatedAt(s.getEvaluatedAt())
                .evaluatedBy(s.getEvaluatedBy())
                .build();
    }
}
