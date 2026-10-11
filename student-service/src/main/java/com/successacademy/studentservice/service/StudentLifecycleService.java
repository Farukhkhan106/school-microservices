package com.successacademy.studentservice.service;

import com.successacademy.studentservice.dto.StudentLifecycleActionRequest;
import com.successacademy.studentservice.dto.StudentLifecycleLogDto;
import com.successacademy.studentservice.model.Student;
import com.successacademy.studentservice.model.StudentLifecycleLog;
import com.successacademy.studentservice.repository.StudentLifecycleLogRepository;
import com.successacademy.studentservice.repository.StudentRepository;
import com.successacademy.studentservice.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentLifecycleService {

    private final StudentRepository studentRepository;
    private final StudentLifecycleLogRepository lifecycleLogRepository;

    @Transactional
    public Student executeLifecycleAction(Long studentId, StudentLifecycleActionRequest request, String actor) {
        String tenantId = TenantContext.getTenantId();
        Student student = studentRepository.findByIdAndTenantId(studentId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        String currentStatus = student.getLifecycleStatus() != null ? student.getLifecycleStatus().toUpperCase() : "ACTIVE";
        String action = request.getAction() != null ? request.getAction().toUpperCase() : "";

        String targetStatus;
        switch (action) {
            case "TRANSFER" -> targetStatus = "TRANSFERRED";
            case "WITHDRAW" -> targetStatus = "WITHDRAWN";
            case "GRADUATE" -> targetStatus = "GRADUATED";
            case "ACTIVATE" -> targetStatus = "ACTIVE";
            case "DEACTIVATE" -> targetStatus = "INACTIVE";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid lifecycle action: " + action);
        }

        validateTransition(currentStatus, targetStatus);

        // Update student
        student.setLifecycleStatus(targetStatus);
        if ("ACTIVE".equals(targetStatus)) {
            student.setStatus("ACTIVE");
        } else {
            student.setStatus("INACTIVE"); // preserve legacy status compatibility
        }
        Student savedStudent = studentRepository.save(student);

        // Record historical audit log
        StudentLifecycleLog logEntry = StudentLifecycleLog.builder()
                .studentId(studentId)
                .tenantId(tenantId)
                .fromStatus(currentStatus)
                .toStatus(targetStatus)
                .action(action)
                .reason(request.getReason().trim())
                .destinationInstitution(request.getDestinationInstitution() != null ? request.getDestinationInstitution().trim() : null)
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .effectiveDate(request.getEffectiveDate() != null ? request.getEffectiveDate() : LocalDate.now())
                .actorUsername(actor != null ? actor : "system")
                .build();

        lifecycleLogRepository.save(logEntry);
        log.info("Student {} lifecycle transitioned from {} to {} via action {} by {} for tenant {}",
                studentId, currentStatus, targetStatus, action, actor, tenantId);

        return savedStudent;
    }

    @Transactional(readOnly = true)
    public List<StudentLifecycleLogDto> getLifecycleHistory(Long studentId) {
        String tenantId = TenantContext.getTenantId();
        return lifecycleLogRepository.findByStudentIdAndTenantIdOrderByPerformedAtDesc(studentId, tenantId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private void validateTransition(String fromStatus, String toStatus) {
        if (fromStatus.equalsIgnoreCase(toStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student is already in status " + toStatus);
        }

        // Terminal or controlled states
        if (Set.of("TRANSFERRED", "WITHDRAWN", "GRADUATED").contains(fromStatus) && !"ACTIVE".equals(toStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid transition: Student is in terminal state '" + fromStatus + "'. Can only be reactivated.");
        }

        if ("GRADUATED".equals(fromStatus) && Set.of("TRANSFER", "WITHDRAW").contains(toStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Graduated students cannot be transferred or withdrawn");
        }
    }

    public StudentLifecycleLogDto mapToDto(StudentLifecycleLog log) {
        return StudentLifecycleLogDto.builder()
                .id(log.getId())
                .studentId(log.getStudentId())
                .tenantId(log.getTenantId())
                .fromStatus(log.getFromStatus())
                .toStatus(log.getToStatus())
                .action(log.getAction())
                .reason(log.getReason())
                .destinationInstitution(log.getDestinationInstitution())
                .notes(log.getNotes())
                .effectiveDate(log.getEffectiveDate())
                .performedBy(log.getPerformedBy())
                .performedAt(log.getPerformedAt())
                .build();
    }
}
