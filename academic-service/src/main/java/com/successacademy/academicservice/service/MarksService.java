package com.successacademy.academicservice.service;

import com.successacademy.academicservice.client.StudentServiceClient;
import com.successacademy.academicservice.dto.*;
import com.successacademy.academicservice.model.*;
import com.successacademy.academicservice.repository.AssessmentScheduleRepository;
import com.successacademy.academicservice.repository.StudentMarkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarksService {

    private final AssessmentScheduleRepository scheduleRepository;
    private final StudentMarkRepository markRepository;
    private final StudentServiceClient studentServiceClient;
    private final GradingService gradingService;
    private final RankingEngine rankingEngine;
    private final AuditLogService auditLogService;

    public ScheduleMarksOverviewResponse getMarksForSchedule(Long scheduleId) {
        AssessmentSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found with ID: " + scheduleId));

        List<StudentMark> existingMarks = markRepository.findByScheduleIdOrderByRollNoAsc(scheduleId);
        Map<Long, StudentMark> markMap = existingMarks.stream()
                .collect(Collectors.toMap(StudentMark::getStudentId, m -> m, (m1, m2) -> m1));

        // Fetch enrolled students from student-service
        List<StudentServiceClient.StudentInfoDto> enrolledStudents = studentServiceClient
                .getStudentsByClassAndSection(schedule.getStudentClass(), schedule.getSection());

        List<StudentMarkResponse> responses = new ArrayList<>();
        Set<Long> processedStudentIds = new HashSet<>();

        if (enrolledStudents != null && !enrolledStudents.isEmpty()) {
            for (StudentServiceClient.StudentInfoDto st : enrolledStudents) {
                processedStudentIds.add(st.getId());
                StudentMark existing = markMap.get(st.getId());
                if (existing != null) {
                    responses.add(mapToMarkResponse(existing));
                } else {
                    String fullName = ((st.getFirstName() != null ? st.getFirstName() : "") + " " +
                            (st.getLastName() != null ? st.getLastName() : "")).trim();
                    responses.add(StudentMarkResponse.builder()
                            .id(null)
                            .scheduleId(schedule.getId())
                            .studentId(st.getId())
                            .studentName(fullName.isEmpty() ? "Student #" + st.getId() : fullName)
                            .rollNo(st.getRollNo())
                            .marksObtained(null)
                            .grade(null)
                            .gradePoint(null)
                            .isAbsent(false)
                            .isPassing(false)
                            .remarks("")
                            .status(schedule.getStatus())
                            .build());
                }
            }
        }

        // Also include any marks that might have been saved previously for students not in current active enrolled list
        for (StudentMark m : existingMarks) {
            if (!processedStudentIds.contains(m.getStudentId())) {
                responses.add(mapToMarkResponse(m));
            }
        }

        // Sort naturally by Roll No
        responses.sort((a, b) -> {
            String rA = a.getRollNo() != null ? a.getRollNo().trim() : "";
            String rB = b.getRollNo() != null ? b.getRollNo().trim() : "";
            try {
                int numA = Integer.parseInt(rA);
                int numB = Integer.parseInt(rB);
                return Integer.compare(numA, numB);
            } catch (NumberFormatException e) {
                return rA.compareToIgnoreCase(rB);
            }
        });

        return ScheduleMarksOverviewResponse.builder()
                .scheduleId(schedule.getId())
                .assessmentId(schedule.getAssessment().getId())
                .assessmentName(schedule.getAssessment().getName())
                .studentClass(schedule.getStudentClass())
                .section(schedule.getSection())
                .subject(schedule.getSubject())
                .component(schedule.getComponent())
                .examDate(schedule.getExamDate())
                .periodNo(schedule.getPeriodNo())
                .teacherId(schedule.getTeacherId())
                .teacherName(schedule.getTeacherName())
                .maxMarks(schedule.getMaxMarks())
                .passMarks(schedule.getPassMarks())
                .status(schedule.getStatus())
                .marks(responses)
                .build();
    }

    @Transactional
    public ScheduleMarksOverviewResponse saveBatchMarks(BatchMarksEntryRequest req, Long actorUserId, String actorRole) {
        AssessmentSchedule schedule = scheduleRepository.findById(req.getScheduleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found with ID: " + req.getScheduleId()));

        // Prevent modification if schedule is LOCKED or PUBLISHED, unless ADMIN
        if ((schedule.getStatus() == MarkStatus.LOCKED || schedule.getAssessment().getStatus() == AssessmentStatus.PUBLISHED)
                && !"ADMIN".equals(actorRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Marks are locked and cannot be modified.");
        }

        boolean isSubmit = "SUBMIT".equalsIgnoreCase(req.getAction());
        BigDecimal maxMarks = schedule.getMaxMarks();
        BigDecimal passMarks = schedule.getPassMarks();
        GradingScheme scheme = schedule.getAssessment().getGradingScheme();

        List<StudentMark> toSave = new ArrayList<>();

        for (MarkEntryItem item : req.getEntries()) {
            if (item.getMarksObtained() != null && item.getMarksObtained().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Marks cannot be negative for student: " + item.getStudentName());
            }

            if (item.getMarksObtained() != null && maxMarks != null && item.getMarksObtained().compareTo(maxMarks) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Marks obtained (" + item.getMarksObtained() + ") cannot exceed maximum marks (" + maxMarks + ") for student: " + item.getStudentName());
            }

            if (isSubmit && !item.isAbsent() && item.getMarksObtained() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please enter marks or mark as absent for student: " + item.getStudentName() + " before submission.");
            }

            StudentMark mark = markRepository.findByScheduleIdAndStudentId(schedule.getId(), item.getStudentId())
                    .orElse(StudentMark.builder()
                            .schedule(schedule)
                            .studentId(item.getStudentId())
                            .build());

            mark.setStudentName(item.getStudentName());
            mark.setRollNo(item.getRollNo());
            mark.setAbsent(item.isAbsent());
            mark.setRemarks(item.getRemarks());
            mark.setEnteredBy(actorUserId);

            if (item.isAbsent()) {
                mark.setMarksObtained(null);
                mark.setGrade("AB");
                mark.setGradePoint(BigDecimal.ZERO);
                mark.setPassing(false);
            } else if (item.getMarksObtained() != null) {
                mark.setMarksObtained(item.getMarksObtained());
                GradingRule rule = gradingService.calculateGrade(item.getMarksObtained(), maxMarks, scheme);
                if (rule != null) {
                    mark.setGrade(rule.getGrade());
                    mark.setGradePoint(rule.getGradePoint());
                    mark.setPassing(rule.isPassing() && item.getMarksObtained().compareTo(passMarks) >= 0);
                } else {
                    mark.setGrade("E");
                    mark.setGradePoint(BigDecimal.ZERO);
                    mark.setPassing(false);
                }
            } else {
                mark.setMarksObtained(null);
                mark.setGrade(null);
                mark.setGradePoint(null);
                mark.setPassing(false);
            }

            mark.setStatus(isSubmit ? MarkStatus.SUBMITTED : MarkStatus.DRAFT);
            toSave.add(mark);
        }

        markRepository.saveAll(toSave);

        if (isSubmit) {
            schedule.setStatus(MarkStatus.SUBMITTED);
            scheduleRepository.save(schedule);
            auditLogService.log(actorUserId, actorRole, "MARKS_SUBMITTED", "AssessmentSchedule", schedule.getId(), null, "Submitted marks for " + toSave.size() + " students", "Teacher submitted marks");
        } else {
            auditLogService.log(actorUserId, actorRole, "MARKS_DRAFT_SAVED", "AssessmentSchedule", schedule.getId(), null, "Saved draft for " + toSave.size() + " students", "Teacher saved draft");
        }

        // Trigger rank update
        rankingEngine.calculateRanksForClassAndSection(schedule.getAssessment().getId(), schedule.getStudentClass(), schedule.getSection());

        return getMarksForSchedule(schedule.getId());
    }

    @Transactional
    public void verifyScheduleMarks(Long scheduleId, Long actorUserId, String actorRole) {
        AssessmentSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found with ID: " + scheduleId));

        schedule.setStatus(MarkStatus.VERIFIED);
        scheduleRepository.save(schedule);

        List<StudentMark> marks = markRepository.findByScheduleIdOrderByRollNoAsc(scheduleId);
        marks.forEach(m -> m.setStatus(MarkStatus.VERIFIED));
        markRepository.saveAll(marks);

        auditLogService.log(actorUserId, actorRole, "MARKS_VERIFIED", "AssessmentSchedule", scheduleId, MarkStatus.SUBMITTED.name(), MarkStatus.VERIFIED.name(), "Admin verified marks");

        rankingEngine.calculateRanksForClassAndSection(schedule.getAssessment().getId(), schedule.getStudentClass(), schedule.getSection());
    }

    @Transactional
    public void lockScheduleMarks(Long scheduleId, Long actorUserId, String actorRole) {
        AssessmentSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found with ID: " + scheduleId));

        schedule.setStatus(MarkStatus.LOCKED);
        scheduleRepository.save(schedule);

        List<StudentMark> marks = markRepository.findByScheduleIdOrderByRollNoAsc(scheduleId);
        marks.forEach(m -> m.setStatus(MarkStatus.LOCKED));
        markRepository.saveAll(marks);

        auditLogService.log(actorUserId, actorRole, "MARKS_LOCKED", "AssessmentSchedule", scheduleId, null, MarkStatus.LOCKED.name(), "Admin locked marks");
    }

    public StudentMarkResponse mapToMarkResponse(StudentMark m) {
        return StudentMarkResponse.builder()
                .id(m.getId())
                .scheduleId(m.getSchedule().getId())
                .studentId(m.getStudentId())
                .studentName(m.getStudentName())
                .rollNo(m.getRollNo())
                .marksObtained(m.getMarksObtained())
                .isAbsent(m.isAbsent())
                .grade(m.getGrade())
                .gradePoint(m.getGradePoint())
                .isPassing(m.isPassing())
                .remarks(m.getRemarks())
                .status(m.getStatus())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}
