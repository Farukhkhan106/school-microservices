package com.successacademy.academicservice.service;

import com.successacademy.academicservice.client.FacultyServiceClient;
import com.successacademy.academicservice.client.StudentServiceClient;
import com.successacademy.academicservice.dto.ActivityCreateRequest;
import com.successacademy.academicservice.dto.ActivityResponse;
import com.successacademy.academicservice.dto.ActivityUpdateRequest;
import com.successacademy.academicservice.model.AcademicActivity;
import com.successacademy.academicservice.model.ActivityStatus;
import com.successacademy.academicservice.model.ActivityType;
import com.successacademy.academicservice.repository.AcademicActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcademicActivityService {

    private final AcademicActivityRepository activityRepository;
    private final FacultyServiceClient facultyServiceClient;
    private final StudentServiceClient studentServiceClient;
    private final AuditLogService auditLogService;

    @Transactional
    public ActivityResponse createActivity(
            ActivityCreateRequest req,
            Long teacherId,
            String teacherName,
            Long actorUserId,
            String actorRole,
            boolean isAdmin
    ) {
        if (!isAdmin) {
            if (teacherId == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing authenticated teacher identity.");
            }
            // Strict Authorization: Teacher must have an ACTIVE assignment for this class, section, and subject
            boolean isAssigned = facultyServiceClient.isTeacherAssigned(
                    teacherId, req.getStudentClass(), req.getSection(), req.getSubject()
            );
            if (!isAssigned) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        String.format("Access denied: You are not assigned to teach %s for Class %s-%s.",
                                req.getSubject(), req.getStudentClass(), req.getSection())
                );
            }
        }

        validateDates(req.getAssignedDate(), req.getDueDate());
        validateMarks(req.getMaxMarks(), req.getPassingMarks());

        ActivityStatus initialStatus = req.getStatus() != null ? req.getStatus() : ActivityStatus.DRAFT;

        AcademicActivity activity = AcademicActivity.builder()
                .title(req.getTitle().trim())
                .description(req.getDescription())
                .activityType(req.getActivityType())
                .studentClass(req.getStudentClass().trim())
                .section(req.getSection().trim().toUpperCase())
                .subject(req.getSubject().trim())
                .teacherId(teacherId != null ? teacherId : 0L)
                .teacherName(teacherName)
                .assignedDate(req.getAssignedDate())
                .dueDate(req.getDueDate())
                .maxMarks(req.getMaxMarks())
                .passingMarks(req.getPassingMarks())
                .status(initialStatus)
                .instructions(req.getInstructions())
                .createdBy(actorUserId)
                .updatedBy(actorUserId)
                .build();

        AcademicActivity saved = activityRepository.save(activity);

        auditLogService.log(
                actorUserId,
                actorRole,
                "ACTIVITY_CREATED",
                "AcademicActivity",
                saved.getId(),
                null,
                saved.getTitle() + " (" + saved.getStatus() + ")",
                "Created activity for " + saved.getStudentClass() + "-" + saved.getSection() + " " + saved.getSubject()
        );

        return mapToResponse(saved);
    }

    @Transactional
    public ActivityResponse updateActivity(
            Long id,
            ActivityUpdateRequest req,
            Long teacherId,
            Long actorUserId,
            String actorRole,
            boolean isAdmin
    ) {
        AcademicActivity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found with ID: " + id));

        // Enforce ownership: only the creator teacher or Admin can update
        if (!isAdmin) {
            if (teacherId == null || !teacherId.equals(activity.getTeacherId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You can only edit your own activities.");
            }
        }

        String oldState = activity.getTitle() + " | " + activity.getStatus() + " | MaxMarks=" + activity.getMaxMarks();

        LocalDate assignedDate = req.getAssignedDate() != null ? req.getAssignedDate() : activity.getAssignedDate();
        LocalDate dueDate = req.getDueDate() != null ? req.getDueDate() : activity.getDueDate();
        validateDates(assignedDate, dueDate);

        BigDecimal maxMarks = req.getMaxMarks() != null ? req.getMaxMarks() : activity.getMaxMarks();
        BigDecimal passMarks = req.getPassingMarks() != null ? req.getPassingMarks() : activity.getPassingMarks();
        validateMarks(maxMarks, passMarks);

        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            activity.setTitle(req.getTitle().trim());
        }
        if (req.getDescription() != null) {
            activity.setDescription(req.getDescription());
        }
        if (req.getActivityType() != null) {
            activity.setActivityType(req.getActivityType());
        }
        activity.setAssignedDate(assignedDate);
        activity.setDueDate(dueDate);
        activity.setMaxMarks(maxMarks);
        activity.setPassingMarks(passMarks);

        if (req.getStatus() != null) {
            activity.setStatus(req.getStatus());
        }
        if (req.getInstructions() != null) {
            activity.setInstructions(req.getInstructions());
        }
        activity.setUpdatedBy(actorUserId);

        AcademicActivity saved = activityRepository.save(activity);

        String newState = saved.getTitle() + " | " + saved.getStatus() + " | MaxMarks=" + saved.getMaxMarks();
        auditLogService.log(
                actorUserId,
                actorRole,
                "ACTIVITY_UPDATED",
                "AcademicActivity",
                saved.getId(),
                oldState,
                newState,
                "Updated activity ID " + saved.getId()
        );

        return mapToResponse(saved);
    }

    @Transactional
    public ActivityResponse updateStatus(
            Long id,
            ActivityStatus newStatus,
            Long teacherId,
            Long actorUserId,
            String actorRole,
            boolean isAdmin
    ) {
        AcademicActivity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found with ID: " + id));

        if (!isAdmin) {
            if (teacherId == null || !teacherId.equals(activity.getTeacherId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You can only change status of your own activities.");
            }
        }

        ActivityStatus oldStatus = activity.getStatus();
        activity.setStatus(newStatus);
        activity.setUpdatedBy(actorUserId);

        AcademicActivity saved = activityRepository.save(activity);

        auditLogService.log(
                actorUserId,
                actorRole,
                "ACTIVITY_" + newStatus.name(),
                "AcademicActivity",
                saved.getId(),
                oldStatus.name(),
                newStatus.name(),
                "Status changed to " + newStatus.name()
        );

        return mapToResponse(saved);
    }

    @Transactional
    public void deleteActivity(Long id, Long teacherId, Long actorUserId, String actorRole, boolean isAdmin) {
        AcademicActivity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found with ID: " + id));

        if (!isAdmin) {
            if (teacherId == null || !teacherId.equals(activity.getTeacherId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You can only delete your own activities.");
            }
        }

        activityRepository.delete(activity);

        auditLogService.log(
                actorUserId,
                actorRole,
                "ACTIVITY_DELETED",
                "AcademicActivity",
                id,
                activity.getTitle(),
                null,
                "Deleted activity ID " + id
        );
    }

    public List<ActivityResponse> getTeacherActivities(
            Long teacherId,
            String studentClass,
            String section,
            String subject,
            ActivityType type,
            ActivityStatus status
    ) {
        if (teacherId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authenticated teacher identity.");
        }
        return activityRepository.findTeacherActivitiesFiltered(teacherId, studentClass, section, subject, type, status)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<ActivityResponse> getStudentActivities(
            Long studentId,
            String subject,
            ActivityType type
    ) {
        if (studentId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authenticated student identity.");
        }

        StudentServiceClient.StudentInfoDto student = studentServiceClient.getStudentById(studentId);
        if (student == null || student.getStudentClass() == null) {
            log.warn("Student record not found or class unassigned for studentId={}", studentId);
            return List.of();
        }

        String studentClass = student.getStudentClass().trim();
        String section = student.getSection() != null ? student.getSection().trim().toUpperCase() : "A";

        // Students ONLY see PUBLISHED activities
        return activityRepository.findStudentActivitiesFiltered(
                studentClass,
                section,
                ActivityStatus.PUBLISHED,
                subject,
                type
        ).stream().map(this::mapToResponse).toList();
    }

    public ActivityResponse getActivityById(Long id, Long actorUserId, String actorRole, Long teacherId, Long studentId) {
        AcademicActivity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found with ID: " + id));

        if ("ADMIN".equalsIgnoreCase(actorRole)) {
            return mapToResponse(activity);
        }

        if ("TEACHER".equalsIgnoreCase(actorRole)) {
            // Teacher can see own activities in any status, or other published activities
            if (teacherId != null && teacherId.equals(activity.getTeacherId())) {
                return mapToResponse(activity);
            }
            if (activity.getStatus() == ActivityStatus.PUBLISHED) {
                return mapToResponse(activity);
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You can only view published activities or your own.");
        }

        if ("STUDENT".equalsIgnoreCase(actorRole)) {
            if (activity.getStatus() != ActivityStatus.PUBLISHED) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found or not published.");
            }
            StudentServiceClient.StudentInfoDto student = studentServiceClient.getStudentById(studentId);
            if (student == null ||
                !activity.getStudentClass().equalsIgnoreCase(student.getStudentClass()) ||
                !activity.getSection().equalsIgnoreCase(student.getSection())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Activity does not belong to your class.");
            }
            return mapToResponse(activity);
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
    }

    private void validateDates(LocalDate assignedDate, LocalDate dueDate) {
        if (assignedDate != null && dueDate != null && dueDate.isBefore(assignedDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Due date cannot be earlier than assigned date.");
        }
    }

    private void validateMarks(BigDecimal maxMarks, BigDecimal passingMarks) {
        if (maxMarks != null && maxMarks.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max marks must be greater than 0.");
        }
        if (passingMarks != null) {
            if (passingMarks.compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passing marks cannot be negative.");
            }
            if (maxMarks != null && passingMarks.compareTo(maxMarks) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passing marks cannot exceed max marks.");
            }
        }
    }

    private ActivityResponse mapToResponse(AcademicActivity a) {
        return ActivityResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .description(a.getDescription())
                .activityType(a.getActivityType())
                .studentClass(a.getStudentClass())
                .section(a.getSection())
                .subject(a.getSubject())
                .teacherId(a.getTeacherId())
                .teacherName(a.getTeacherName())
                .assignedDate(a.getAssignedDate())
                .dueDate(a.getDueDate())
                .maxMarks(a.getMaxMarks())
                .passingMarks(a.getPassingMarks())
                .status(a.getStatus())
                .instructions(a.getInstructions())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
