package com.successacademy.academicservice.service;

import com.successacademy.academicservice.dto.ScheduleCreateRequest;
import com.successacademy.academicservice.dto.ScheduleResponse;
import com.successacademy.academicservice.model.*;
import com.successacademy.academicservice.repository.AssessmentRepository;
import com.successacademy.academicservice.repository.AssessmentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssessmentScheduleService {

    private final AssessmentScheduleRepository scheduleRepository;
    private final AssessmentRepository assessmentRepository;
    private final AuditLogService auditLogService;

    public List<ScheduleResponse> getSchedulesByAssessment(Long assessmentId) {
        return scheduleRepository.findByAssessmentId(assessmentId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<ScheduleResponse> getSchedulesByTeacher(Long teacherId) {
        return scheduleRepository.findByTeacherId(teacherId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<ScheduleResponse> getSchedulesByClassAndSection(Long assessmentId, String studentClass, String section) {
        return scheduleRepository.findByAssessmentIdAndStudentClassAndSection(assessmentId, studentClass, section).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ScheduleResponse getScheduleById(Long id) {
        AssessmentSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment schedule not found with ID: " + id));
        return mapToResponse(schedule);
    }

    @Transactional
    public ScheduleResponse createSchedule(ScheduleCreateRequest req, Long actorUserId, String actorRole) {
        Assessment assessment = assessmentRepository.findById(req.getAssessmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found with ID: " + req.getAssessmentId()));

        AssessmentComponent component = req.getComponent() != null ? req.getComponent() : AssessmentComponent.THEORY;

        if (scheduleRepository.findByAssessmentIdAndStudentClassAndSectionAndSubject(
                assessment.getId(), req.getStudentClass().trim(), req.getSection().trim(), req.getSubject().trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Schedule for subject '" + req.getSubject() + "' already exists for Class " + req.getStudentClass() + "-" + req.getSection() + " in this assessment.");
        }

        AssessmentSchedule schedule = AssessmentSchedule.builder()
                .assessment(assessment)
                .studentClass(req.getStudentClass().trim())
                .section(req.getSection().trim())
                .subject(req.getSubject().trim())
                .component(component)
                .examDate(req.getExamDate())
                .periodNo(req.getPeriodNo())
                .teacherId(req.getTeacherId())
                .teacherName(req.getTeacherName() != null ? req.getTeacherName().trim() : "Faculty")
                .maxMarks(req.getMaxMarks())
                .passMarks(req.getPassMarks())
                .status(MarkStatus.DRAFT)
                .build();

        AssessmentSchedule saved = scheduleRepository.save(schedule);
        auditLogService.log(actorUserId, actorRole, "SCHEDULE_CREATED", "AssessmentSchedule", saved.getId(), null, saved.getSubject() + " " + saved.getStudentClass() + "-" + saved.getSection(), "Created schedule");

        return mapToResponse(saved);
    }

    @Transactional
    public void deleteSchedule(Long id, Long actorUserId, String actorRole) {
        AssessmentSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment schedule not found with ID: " + id));

        if (!schedule.getMarks().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete schedule that already contains student marks.");
        }

        scheduleRepository.delete(schedule);
        auditLogService.log(actorUserId, actorRole, "SCHEDULE_DELETED", "AssessmentSchedule", id, schedule.getSubject(), null, "Deleted schedule");
    }

    public ScheduleResponse mapToResponse(AssessmentSchedule s) {
        int enteredCount = s.getMarks() != null ? s.getMarks().size() : 0;

        return ScheduleResponse.builder()
                .id(s.getId())
                .assessmentId(s.getAssessment().getId())
                .assessmentName(s.getAssessment().getName())
                .studentClass(s.getStudentClass())
                .section(s.getSection())
                .subject(s.getSubject())
                .component(s.getComponent())
                .componentDisplayName(s.getComponent() != null ? s.getComponent().getDisplayName() : "Theory")
                .examDate(s.getExamDate())
                .periodNo(s.getPeriodNo())
                .teacherId(s.getTeacherId())
                .teacherName(s.getTeacherName())
                .maxMarks(s.getMaxMarks())
                .passMarks(s.getPassMarks())
                .status(s.getStatus())
                .enrolledStudentsCount(enteredCount)
                .enteredMarksCount(enteredCount)
                .build();
    }
}
