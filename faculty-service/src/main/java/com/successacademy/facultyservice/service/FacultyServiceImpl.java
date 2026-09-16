package com.successacademy.facultyservice.service;

import com.successacademy.facultyservice.client.AuthServiceClient;
import com.successacademy.facultyservice.dto.AbsenceOverviewResponse;
import com.successacademy.facultyservice.dto.AssignmentRequest;
import com.successacademy.facultyservice.dto.AssignmentResponse;
import com.successacademy.facultyservice.dto.FacultyRequest;
import com.successacademy.facultyservice.dto.FacultyResponse;
import com.successacademy.facultyservice.dto.ScheduleRequest;
import com.successacademy.facultyservice.dto.ScheduleResponse;
import com.successacademy.facultyservice.dto.SubstituteRequest;
import com.successacademy.facultyservice.dto.SubstituteResponse;
import com.successacademy.facultyservice.dto.TeacherProfileResponse;
import com.successacademy.facultyservice.exception.ConflictException;
import com.successacademy.facultyservice.exception.NotFoundException;
import com.successacademy.facultyservice.exception.UnauthorizedException;
import com.successacademy.facultyservice.model.ClassSchedule;
import com.successacademy.facultyservice.model.Faculty;
import com.successacademy.facultyservice.model.TeacherClassAssignment;
import com.successacademy.facultyservice.model.TeacherSubstitute;
import com.successacademy.facultyservice.repository.ClassScheduleRepository;
import com.successacademy.facultyservice.repository.FacultyRepository;
import com.successacademy.facultyservice.repository.TeacherClassAssignmentRepository;
import com.successacademy.facultyservice.repository.TeacherSubstituteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FacultyServiceImpl implements FacultyService {

    private static final List<String> DAYS = List.of(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");

    private final FacultyRepository repository;
    private final AuthServiceClient authServiceClient;
    private final TeacherClassAssignmentRepository assignmentRepository;
    private final ClassScheduleRepository scheduleRepository;
    private final TeacherSubstituteRepository substituteRepository;

    @Override
    public FacultyResponse addFaculty(FacultyRequest request) {
        Faculty faculty = mapToEntity(request);
        Faculty saved = repository.save(faculty);

        // Auto-create login account in auth-service
        Long authUserId = authServiceClient.createTeacherUser(
            saved.getId(),
            saved.getName(),
            saved.getEmail()
        );

        // Store the generated auth userId back into faculty record
        if (authUserId != null) {
            saved.setUserId(authUserId);
            repository.save(saved);
        }

        return mapToResponse(saved);
    }

    @Override
    public FacultyResponse updateFaculty(Long id, FacultyRequest request) {
        Faculty faculty = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found with id: " + id));

        String oldStatus = faculty.getStatus();
        faculty.setName(request.getName());
        faculty.setEmail(request.getEmail());
        faculty.setPhone(request.getPhone());
        faculty.setDesignation(request.getDesignation());
        faculty.setQualification(request.getQualification());
        faculty.setExperience(request.getExperience());
        faculty.setSubjects(request.getSubjects());
        faculty.setClassTeacherOf(request.getClassTeacherOf());
        faculty.setPhotoUrl(request.getPhotoUrl());
        faculty.setStatus(request.getStatus());
        if (request.getUserId() != null) faculty.setUserId(request.getUserId());

        Faculty saved = repository.save(faculty);

        if (request.getStatus() != null && !request.getStatus().equalsIgnoreCase(oldStatus)) {
            authServiceClient.updateTeacherStatus(saved.getId(), request.getStatus());
        }

        return mapToResponse(saved);
    }

    @Override
    public void deleteFaculty(Long id) {
        Faculty faculty = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found with id: " + id));
        // Soft deactivate faculty to preserve historical data & assignments
        faculty.setStatus("Inactive");
        faculty.setClassTeacherOf(null);
        repository.save(faculty);
        authServiceClient.updateTeacherStatus(id, "INACTIVE");
    }

    @Override
    public List<FacultyResponse> getAllFaculty() {
        return repository.findAll().stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(this::mapToResponse).toList();
    }

    @Override
    public List<com.successacademy.facultyservice.dto.PublicFacultyResponse> getActiveFaculty() {
        return repository.findByStatusIgnoreCase("Active").stream()
                .map(this::mapToPublicResponse).toList();
    }

    @Override
    public FacultyResponse getFacultyById(Long id) {
        return mapToResponse(repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found with id: " + id)));
    }

    @Override
    public FacultyResponse getFacultyByUserId(Long userId) {
        return mapToResponse(repository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Faculty not found for userId: " + userId)));
    }

    @Override
    public List<FacultyResponse> searchByName(String keyword) {
        return repository.findByNameContainingIgnoreCase(keyword).stream()
                .map(this::mapToResponse).toList();
    }

    @Override
    public List<FacultyResponse> filterBySubject(String subject) {
        return repository.findBySubjectsContainingIgnoreCase(subject).stream()
                .map(this::mapToResponse).toList();
    }

    // ── TEACHER MANAGEMENT FOUNDATION ────────────────────────────

    private Faculty resolveTeacher(Long authUserId) {
        if (authUserId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        // 1. Direct lookup by linked userId
        var byUser = repository.findByUserId(authUserId);
        if (byUser.isPresent()) return byUser.get();

        // 2. Direct lookup by ID (e.g. if authUserId equals faculty.id)
        var byId = repository.findById(authUserId);
        if (byId.isPresent()) {
            Faculty f = byId.get();
            if (f.getUserId() == null) {
                f.setUserId(authUserId);
                repository.save(f);
            }
            return f;
        }

        // 3. Auto-link to first active unlinked faculty record
        var all = repository.findAll();
        for (Faculty f : all) {
            if (f.getUserId() == null && "Active".equalsIgnoreCase(f.getStatus())) {
                f.setUserId(authUserId);
                return repository.save(f);
            }
        }

        throw new NotFoundException("No teacher profile is linked to this account");
    }

    @Override
    public TeacherProfileResponse getTeacherProfile(Long authUserId) {
        Faculty f = resolveTeacher(authUserId);
        List<AssignmentResponse> assignments = assignmentRepository
                .findByTeacherIdOrderById(f.getId()).stream().map(this::toAssignmentResponse).toList();
        List<ScheduleResponse> schedule = scheduleRepository
                .findByTeacherIdOrderByDayOfWeekAscPeriodNoAsc(f.getId()).stream().map(this::toScheduleResponse).toList();

        Set<String> allowed = new LinkedHashSet<>();
        if (f.getClassTeacherOf() != null && !f.getClassTeacherOf().isBlank()) allowed.add(f.getClassTeacherOf());
        assignments.stream()
                .filter(a -> "Active".equalsIgnoreCase(a.getStatus()))
                .forEach(a -> allowed.add(a.getStudentClass() + "-" + a.getSection()));

        LocalDate today = LocalDate.now();

        // 1. Check if covering as substitute today
        List<SubstituteResponse> todaySubs = substituteRepository
                .findBySubstituteTeacherIdAndDateAndStatus(f.getId(), today, "ASSIGNED")
                .stream()
                .map(this::toSubstituteResponse)
                .toList();

        // Add today's substitute classes into allowed classes
        todaySubs.forEach(s -> allowed.add(s.getStudentClass() + "-" + s.getSection()));

        // 2. Check if marked absent today
        List<TeacherSubstitute> myAbsences = substituteRepository.findByAbsentTeacherIdAndDate(f.getId(), today)
                .stream()
                .filter(s -> !"CANCELLED".equalsIgnoreCase(s.getStatus()))
                .toList();
        boolean isAbsentToday = !myAbsences.isEmpty();
        String absenceReason = isAbsentToday ? myAbsences.get(0).getReason() : null;

        return TeacherProfileResponse.builder()
                .faculty(mapToResponse(f))
                .assignments(assignments)
                .schedule(schedule)
                .allowedClasses(new ArrayList<>(allowed))
                .todaySubstitutions(todaySubs)
                .absentToday(isAbsentToday)
                .absenceReason(absenceReason)
                .build();
    }

    // ── CLASS TEACHER (at most one per class-section) ────────────

    @Override
    public FacultyResponse assignClassTeacher(Long teacherId, String classTeacherOf, boolean replace) {
        Faculty teacher = getTeacher(teacherId);

        if (classTeacherOf == null || classTeacherOf.isBlank()) {
            teacher.setClassTeacherOf(null); // explicit unassign
            return mapToResponse(repository.save(teacher));
        }

        String ct = classTeacherOf.trim();
        if (!"Active".equalsIgnoreCase(teacher.getStatus())) {
            throw new ConflictException(teacher.getName() + " is inactive — activate the teacher first");
        }

        var current = repository.findByClassTeacherOf(ct);
        if (current.isPresent() && !current.get().getId().equals(teacherId)) {
            if (!replace) {
                // Never silently steal a class from another teacher
                throw new ConflictException("Class " + ct + " is already assigned to " + current.get().getName());
            }
            Faculty previous = current.get();
            previous.setClassTeacherOf(null);
            repository.save(previous);
        }

        teacher.setClassTeacherOf(ct);
        return mapToResponse(repository.save(teacher));
    }

    // ── SUBJECT-TEACHER ASSIGNMENTS ──────────────────────────────

    @Override
    public List<AssignmentResponse> getAllAssignments() {
        return assignmentRepository.findAll().stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(this::toAssignmentResponse).toList();
    }

    @Override
    public List<AssignmentResponse> getAssignmentsByTeacher(Long teacherId) {
        return assignmentRepository.findByTeacherIdOrderById(teacherId).stream()
                .map(this::toAssignmentResponse).toList();
    }

    @Override
    public AssignmentResponse addAssignment(AssignmentRequest r) {
        validateAssignment(r);
        Faculty teacher = getTeacher(r.getTeacherId());
        if (!"Active".equalsIgnoreCase(teacher.getStatus())) {
            throw new ConflictException(teacher.getName() + " is inactive — activate the teacher first");
        }
        if (assignmentRepository.existsByTeacherIdAndStudentClassAndSectionAndSubjectIgnoreCase(
                r.getTeacherId(), r.getStudentClass().trim(), r.getSection().trim(), r.getSubject().trim())) {
            throw new ConflictException(teacher.getName() + " already teaches " + r.getSubject().trim()
                    + " in Class " + r.getStudentClass().trim() + "-" + r.getSection().trim());
        }
        TeacherClassAssignment a = TeacherClassAssignment.builder()
                .teacherId(r.getTeacherId())
                .studentClass(r.getStudentClass().trim())
                .section(r.getSection().trim())
                .subject(r.getSubject().trim())
                .status(r.getStatus() != null && !r.getStatus().isBlank() ? r.getStatus() : "Active")
                .build();
        return toAssignmentResponse(assignmentRepository.save(a));
    }

    @Override
    public AssignmentResponse updateAssignment(Long id, AssignmentRequest r) {
        TeacherClassAssignment a = assignmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Assignment not found with id: " + id));
        validateAssignment(r);
        boolean unchanged = a.getTeacherId().equals(r.getTeacherId())
                && a.getStudentClass().equalsIgnoreCase(r.getStudentClass().trim())
                && a.getSection().equalsIgnoreCase(r.getSection().trim())
                && a.getSubject().equalsIgnoreCase(r.getSubject().trim());
        if (!unchanged && assignmentRepository.existsByTeacherIdAndStudentClassAndSectionAndSubjectIgnoreCase(
                r.getTeacherId(), r.getStudentClass().trim(), r.getSection().trim(), r.getSubject().trim())) {
            throw new ConflictException("This assignment already exists");
        }
        a.setTeacherId(r.getTeacherId());
        a.setStudentClass(r.getStudentClass().trim());
        a.setSection(r.getSection().trim());
        a.setSubject(r.getSubject().trim());
        if (r.getStatus() != null && !r.getStatus().isBlank()) a.setStatus(r.getStatus());
        return toAssignmentResponse(assignmentRepository.save(a));
    }

    @Override
    public void deleteAssignment(Long id) {
        if (!assignmentRepository.existsById(id)) {
            throw new NotFoundException("Assignment not found with id: " + id);
        }
        assignmentRepository.deleteById(id);
    }

    private void validateAssignment(AssignmentRequest r) {
        if (r == null || r.getTeacherId() == null) throw new ConflictException("Teacher is required");
        if (blank(r.getStudentClass()) || blank(r.getSection()) || blank(r.getSubject())) {
            throw new ConflictException("Class, section and subject are required");
        }
    }

    private Faculty getTeacher(Long teacherId) {
        return repository.findById(teacherId)
                .orElseThrow(() -> new NotFoundException("Teacher not found with id: " + teacherId));
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }

    // ── WEEKLY TIMETABLE ─────────────────────────────────────────

    @Override
    public List<ScheduleResponse> getAllSchedules() {
        return scheduleRepository.findAllByOrderByDayOfWeekAscPeriodNoAsc().stream()
                .map(this::toScheduleResponse).toList();
    }

    @Override
    public List<ScheduleResponse> getSchedulesByClass(String studentClass, String section) {
        return scheduleRepository
                .findByStudentClassAndSectionIgnoreCaseOrderByPeriodNoAsc(studentClass, section).stream()
                .map(this::toScheduleResponse).toList();
    }

    @Override
    public List<ScheduleResponse> getSchedulesByTeacher(Long teacherId) {
        return scheduleRepository.findByTeacherIdOrderByDayOfWeekAscPeriodNoAsc(teacherId).stream()
                .map(this::toScheduleResponse).toList();
    }

    @Override
    public ScheduleResponse addSchedule(ScheduleRequest r) {
        validateSchedule(r);
        Faculty teacher = getTeacher(r.getTeacherId());
        if (!"Active".equalsIgnoreCase(teacher.getStatus())) {
            throw new ConflictException(teacher.getName() + " is inactive — activate the teacher first");
        }
        String day = normalizeDay(r.getDayOfWeek());
        int period = r.getPeriodNo();
        String cls = r.getStudentClass().trim();
        String sec = r.getSection().trim();

        // RULE: one teacher cannot teach two classes in the same period
        if (scheduleRepository.existsByDayOfWeekIgnoreCaseAndPeriodNoAndTeacherId(day, period, r.getTeacherId())) {
            ClassSchedule clash = scheduleRepository
                    .findByTeacherIdOrderByDayOfWeekAscPeriodNoAsc(r.getTeacherId()).stream()
                    .filter(s -> s.getDayOfWeek().equalsIgnoreCase(day) && s.getPeriodNo() == period)
                    .findFirst().orElse(null);
            throw new ConflictException(teacher.getName() + " is already assigned to Class "
                    + (clash == null ? "?" : clash.getClassSection()) + " during " + day + " Period " + period);
        }

        // RULE: one class cannot have two subjects in the same period
        if (scheduleRepository.existsByDayOfWeekIgnoreCaseAndPeriodNoAndStudentClassAndSectionIgnoreCase(
                day, period, cls, sec)) {
            ClassSchedule clash = scheduleRepository
                    .findByStudentClassAndSectionIgnoreCaseOrderByPeriodNoAsc(cls, sec).stream()
                    .filter(s -> s.getDayOfWeek().equalsIgnoreCase(day) && s.getPeriodNo() == period)
                    .findFirst().orElse(null);
            throw new ConflictException("Class " + cls + "-" + sec + " already has "
                    + (clash == null ? "a subject" : clash.getSubject()) + " scheduled for " + day + " Period " + period);
        }

        ClassSchedule s = ClassSchedule.builder()
                .dayOfWeek(day).periodNo(period)
                .startTime(blank(r.getStartTime()) ? null : r.getStartTime().trim())
                .endTime(blank(r.getEndTime()) ? null : r.getEndTime().trim())
                .studentClass(cls).section(sec)
                .subject(r.getSubject().trim()).teacherId(r.getTeacherId())
                .build();
        try {
            return toScheduleResponse(scheduleRepository.save(s));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Class " + cls + "-" + sec
                    + " already has a subject scheduled for " + day + " Period " + period);
        }
    }

    @Override
    public ScheduleResponse updateSchedule(Long id, ScheduleRequest r) {
        ClassSchedule s = scheduleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Schedule entry not found with id: " + id));
        validateSchedule(r);
        String day = normalizeDay(r.getDayOfWeek());
        int period = r.getPeriodNo();
        String cls = r.getStudentClass().trim();
        String sec = r.getSection().trim();

        boolean teacherClash = scheduleRepository.findByTeacherIdOrderByDayOfWeekAscPeriodNoAsc(r.getTeacherId())
                .stream().anyMatch(x -> !x.getId().equals(id)
                        && x.getDayOfWeek().equalsIgnoreCase(day) && x.getPeriodNo() == period);
        if (teacherClash) {
            throw new ConflictException("Teacher already has a class during " + day + " Period " + period);
        }
        boolean classClash = scheduleRepository
                .findByStudentClassAndSectionIgnoreCaseOrderByPeriodNoAsc(cls, sec).stream()
                .anyMatch(x -> !x.getId().equals(id)
                        && x.getDayOfWeek().equalsIgnoreCase(day) && x.getPeriodNo() == period);
        if (classClash) {
            throw new ConflictException("Class " + cls + "-" + sec + " already has a subject scheduled for "
                    + day + " Period " + period);
        }

        s.setDayOfWeek(day);
        s.setPeriodNo(period);
        s.setStartTime(blank(r.getStartTime()) ? null : r.getStartTime().trim());
        s.setEndTime(blank(r.getEndTime()) ? null : r.getEndTime().trim());
        s.setStudentClass(cls);
        s.setSection(sec);
        s.setSubject(r.getSubject().trim());
        s.setTeacherId(r.getTeacherId());
        return toScheduleResponse(scheduleRepository.save(s));
    }

    @Override
    public void deleteSchedule(Long id) {
        if (!scheduleRepository.existsById(id)) {
            throw new NotFoundException("Schedule entry not found with id: " + id);
        }
        scheduleRepository.deleteById(id);
    }

    private void validateSchedule(ScheduleRequest r) {
        if (r == null || r.getTeacherId() == null) throw new ConflictException("Teacher is required");
        if (blank(r.getDayOfWeek()) || blank(r.getStudentClass()) || blank(r.getSection()) || blank(r.getSubject())) {
            throw new ConflictException("Day, class, section and subject are required");
        }
        if (r.getPeriodNo() < 1 || r.getPeriodNo() > 12) {
            throw new ConflictException("Period number must be between 1 and 12");
        }
        normalizeDay(r.getDayOfWeek());
    }

    private String normalizeDay(String day) {
        String d = day.trim();
        return DAYS.stream().filter(x -> x.equalsIgnoreCase(d)).findFirst()
                .orElseThrow(() -> new ConflictException("Invalid day: " + day + " (use Monday–Sunday)"));
    }

    // ── ACCESS CHECKS (used by student & attendance services) ────

    @Override
    public List<String> getAllowedClassSections(Long authUserId) {
        Faculty f = resolveTeacher(authUserId);
        Set<String> allowed = new LinkedHashSet<>();
        if (f.getClassTeacherOf() != null && !f.getClassTeacherOf().isBlank()) allowed.add(f.getClassTeacherOf());
        assignmentRepository.findByTeacherIdOrderById(f.getId()).stream()
                .filter(a -> "Active".equalsIgnoreCase(a.getStatus()))
                .forEach(a -> allowed.add(a.getStudentClass() + "-" + a.getSection()));

        // Also add classes covering as active substitute today
        substituteRepository.findBySubstituteTeacherIdAndDateAndStatus(f.getId(), LocalDate.now(), "ASSIGNED")
                .forEach(s -> allowed.add(s.getStudentClass() + "-" + s.getSection()));

        return new ArrayList<>(allowed);
    }

    @Override
    public boolean hasAnyAccess(Long authUserId, String studentClass, String section) {
        try {
            Faculty f = resolveTeacher(authUserId);
            String cs = studentClass + "-" + section;
            if (cs.equalsIgnoreCase(f.getClassTeacherOf())) return true;
            if (!assignmentRepository.findByTeacherIdAndStudentClassAndSectionIgnoreCase(f.getId(), studentClass, section).isEmpty()) {
                return true;
            }
            // Active substitute cover today
            return !substituteRepository.findActiveCoversForClass(f.getId(), studentClass, section, LocalDate.now()).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean hasClassTeacherAccess(Long authUserId, String studentClass, String section) {
        try {
            Faculty f = resolveTeacher(authUserId);
            if ((studentClass + "-" + section).equalsIgnoreCase(f.getClassTeacherOf())) {
                return true;
            }
            // Active substitute Class Teacher cover today
            return !substituteRepository.findActiveClassTeacherCovers(f.getId(), studentClass, section, LocalDate.now()).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    // ── TEACHER ABSENCE & SUBSTITUTE WORKFLOW IMPLEMENTATION ──────

    @Override
    public SubstituteResponse markAbsenceAndAssignSubstitute(SubstituteRequest request) {
        if (request.getAbsentTeacherId() == null) {
            throw new ConflictException("Absent teacher is required");
        }
        Faculty absentTeacher = getTeacher(request.getAbsentTeacherId());
        if (request.getDate() == null) {
            throw new ConflictException("Date is required");
        }
        if (blank(request.getStudentClass()) || blank(request.getSection())) {
            throw new ConflictException("Class and section are required");
        }

        Long subId = request.getSubstituteTeacherId();
        Faculty subTeacher = null;

        if (subId != null) {
            if (subId.equals(request.getAbsentTeacherId())) {
                throw new ConflictException("A teacher cannot be assigned as their own substitute");
            }
            subTeacher = getTeacher(subId);
            if (!"Active".equalsIgnoreCase(subTeacher.getStatus())) {
                throw new ConflictException("Substitute teacher " + subTeacher.getName() + " is inactive");
            }

            // ── CONFLICT VALIDATION ──
            String dayOfWeek = normalizeDay(request.getDate().getDayOfWeek().name());
            Integer periodNo = request.getPeriodNo();

            if (periodNo != null) {
                // 1. Check substitute's permanent timetable on that day of week & period
                var permanentClash = scheduleRepository.findByTeacherIdOrderByDayOfWeekAscPeriodNoAsc(subId)
                        .stream()
                        .filter(s -> s.getDayOfWeek().equalsIgnoreCase(dayOfWeek) && s.getPeriodNo() == periodNo)
                        .findFirst();
                if (permanentClash.isPresent()) {
                    var clash = permanentClash.get();
                    throw new ConflictException(subTeacher.getName() + " is already scheduled for Class "
                            + clash.getStudentClass() + "-" + clash.getSection() + " (" + clash.getSubject()
                            + ") during Period " + periodNo + " on " + dayOfWeek + "s");
                }

                // 2. Check substitute's other substitute assignments on the same date & period
                var clashingSubs = substituteRepository.findClashingSubstitutions(subId, request.getDate(), periodNo);
                if (!clashingSubs.isEmpty()) {
                    var clash = clashingSubs.get(0);
                    throw new ConflictException(subTeacher.getName() + " is already assigned as a substitute for Class "
                            + clash.getStudentClass() + "-" + clash.getSection() + " during Period " + periodNo
                            + " on " + request.getDate());
                }
            }
        }

        TeacherSubstitute entity = TeacherSubstitute.builder()
                .absentTeacherId(request.getAbsentTeacherId())
                .substituteTeacherId(subId)
                .date(request.getDate())
                .studentClass(request.getStudentClass().trim())
                .section(request.getSection().trim())
                .subject(request.getSubject() != null && !request.getSubject().isBlank() ? request.getSubject().trim() : "General")
                .periodNo(request.getPeriodNo())
                .classTeacherCover(Boolean.TRUE.equals(request.getClassTeacherCover()))
                .reason(request.getReason() != null ? request.getReason().trim() : "Teacher Absent")
                .status(subId != null ? "ASSIGNED" : "ABSENT")
                .scheduleId(request.getScheduleId())
                .build();

        TeacherSubstitute saved = substituteRepository.save(entity);
        return toSubstituteResponse(saved);
    }

    @Override
    public void cancelSubstitute(Long id) {
        TeacherSubstitute sub = substituteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Substitute record not found with id: " + id));
        sub.setStatus("CANCELLED");
        substituteRepository.save(sub);
    }

    @Override
    public List<SubstituteResponse> getSubstitutesByDate(LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now();
        return substituteRepository.findByDateOrderByCreatedAtDesc(d)
                .stream()
                .map(this::toSubstituteResponse)
                .toList();
    }

    @Override
    public List<SubstituteResponse> getSubstitutesForTeacher(Long teacherId) {
        return substituteRepository.findByAbsentTeacherIdOrderByDateDesc(teacherId)
                .stream()
                .map(this::toSubstituteResponse)
                .toList();
    }

    @Override
    public AbsenceOverviewResponse getAbsenceOverview(LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now();
        List<TeacherSubstitute> list = substituteRepository.findByDateOrderByCreatedAtDesc(d);

        Set<Long> absentTeacherIds = new LinkedHashSet<>();
        int covered = 0;
        int uncovered = 0;

        for (TeacherSubstitute s : list) {
            if (!"CANCELLED".equalsIgnoreCase(s.getStatus())) {
                absentTeacherIds.add(s.getAbsentTeacherId());
                if ("ASSIGNED".equalsIgnoreCase(s.getStatus()) && s.getSubstituteTeacherId() != null) {
                    covered++;
                } else {
                    uncovered++;
                }
            }
        }

        return AbsenceOverviewResponse.builder()
                .date(d)
                .totalAbsentTeachers(absentTeacherIds.size())
                .totalPeriodsCovered(covered)
                .totalPeriodsUncovered(uncovered)
                .substitutions(list.stream().map(this::toSubstituteResponse).toList())
                .build();
    }

    @Override
    public List<SubstituteResponse> getMySubstitutions(Long authUserId, LocalDate date) {
        Faculty f = resolveTeacher(authUserId);
        LocalDate d = date != null ? date : LocalDate.now();
        return substituteRepository.findBySubstituteTeacherIdAndDateAndStatus(f.getId(), d, "ASSIGNED")
                .stream()
                .map(this::toSubstituteResponse)
                .toList();
    }

    private SubstituteResponse toSubstituteResponse(TeacherSubstitute s) {
        Faculty absent = repository.findById(s.getAbsentTeacherId()).orElse(null);
        Faculty sub = s.getSubstituteTeacherId() != null ? repository.findById(s.getSubstituteTeacherId()).orElse(null) : null;

        return SubstituteResponse.builder()
                .id(s.getId())
                .absentTeacherId(s.getAbsentTeacherId())
                .absentTeacherName(absent != null ? absent.getName() : "Unknown")
                .substituteTeacherId(s.getSubstituteTeacherId())
                .substituteTeacherName(sub != null ? sub.getName() : "Unassigned")
                .date(s.getDate())
                .studentClass(s.getStudentClass())
                .section(s.getSection())
                .subject(s.getSubject())
                .periodNo(s.getPeriodNo())
                .classTeacherCover(s.isClassTeacherCover())
                .reason(s.getReason())
                .status(s.getStatus())
                .scheduleId(s.getScheduleId())
                .createdAt(s.getCreatedAt())
                .build();
    }

    // ─── HELPERS ────────────────────────────────────────────────

    private AssignmentResponse toAssignmentResponse(TeacherClassAssignment a) {
        Faculty t = repository.findById(a.getTeacherId()).orElse(null);
        return AssignmentResponse.builder()
                .id(a.getId()).teacherId(a.getTeacherId())
                .teacherName(t != null ? t.getName() : "Unknown")
                .studentClass(a.getStudentClass()).section(a.getSection())
                .subject(a.getSubject()).status(a.getStatus())
                .build();
    }

    private ScheduleResponse toScheduleResponse(ClassSchedule s) {
        Faculty t = repository.findById(s.getTeacherId()).orElse(null);
        return ScheduleResponse.builder()
                .id(s.getId()).dayOfWeek(s.getDayOfWeek()).periodNo(s.getPeriodNo())
                .startTime(s.getStartTime()).endTime(s.getEndTime())
                .studentClass(s.getStudentClass()).section(s.getSection())
                .subject(s.getSubject()).teacherId(s.getTeacherId())
                .teacherName(t != null ? t.getName() : "Unknown")
                .build();
    }

    private Faculty mapToEntity(FacultyRequest r) {
        return Faculty.builder()
                .name(r.getName()).email(r.getEmail()).phone(r.getPhone())
                .designation(r.getDesignation()).qualification(r.getQualification())
                .experience(r.getExperience()).subjects(r.getSubjects())
                .classTeacherOf(r.getClassTeacherOf()).photoUrl(r.getPhotoUrl())
                .status(r.getStatus() != null ? r.getStatus() : "Active")
                .userId(r.getUserId())
                .build();
    }

    private FacultyResponse mapToResponse(Faculty f) {
        List<String> subjectList = (f.getSubjects() != null && !f.getSubjects().isBlank())
                ? Arrays.stream(f.getSubjects().split(",")).map(String::trim).toList()
                : List.of();

        List<String> assignedClasses = assignmentRepository.findByTeacherIdOrderById(f.getId())
                .stream()
                .filter(a -> "Active".equalsIgnoreCase(a.getStatus()))
                .map(a -> a.getStudentClass() + "-" + a.getSection())
                .distinct()
                .toList();

        String today = java.time.LocalDate.now().getDayOfWeek().getDisplayName(
                java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);
        int todayCount = (int) scheduleRepository.findByTeacherIdOrderByDayOfWeekAscPeriodNoAsc(f.getId())
                .stream()
                .filter(s -> s.getDayOfWeek().equalsIgnoreCase(today))
                .count();

        return FacultyResponse.builder()
                .id(f.getId()).name(f.getName()).email(f.getEmail()).phone(f.getPhone())
                .designation(f.getDesignation()).qualification(f.getQualification())
                .experience(f.getExperience()).subjects(subjectList)
                .classTeacherOf(f.getClassTeacherOf()).photoUrl(f.getPhotoUrl())
                .status(f.getStatus()).userId(f.getUserId())
                .assignedClasses(assignedClasses)
                .todayClassesCount(todayCount)
                .build();
    }

    private com.successacademy.facultyservice.dto.PublicFacultyResponse mapToPublicResponse(Faculty f) {
        List<String> subjectList = (f.getSubjects() != null && !f.getSubjects().isBlank())
                ? Arrays.stream(f.getSubjects().split(",")).map(String::trim).toList()
                : List.of();

        return com.successacademy.facultyservice.dto.PublicFacultyResponse.builder()
                .id(f.getId())
                .name(f.getName())
                .designation(f.getDesignation())
                .qualification(f.getQualification())
                .experience(f.getExperience())
                .subjects(subjectList)
                .classTeacherOf(f.getClassTeacherOf())
                .photoUrl(f.getPhotoUrl())
                .status(f.getStatus())
                .build();
    }

    @Override
    public java.util.Map<String, Object> checkDeactivation(Long id) {
        Faculty f = getTeacher(id);
        String ct = f.getClassTeacherOf();
        List<TeacherClassAssignment> assignments = assignmentRepository.findByTeacherIdOrderById(id);
        List<ClassSchedule> schedules = scheduleRepository.findByTeacherIdOrderByDayOfWeekAscPeriodNoAsc(id);

        List<String> warnings = new ArrayList<>();
        if (ct != null && !ct.isBlank()) {
            warnings.add("Class Teacher of " + ct);
        }
        if (!assignments.isEmpty()) {
            warnings.add(assignments.size() + " teaching assignment" + (assignments.size() > 1 ? "s" : ""));
        }
        if (!schedules.isEmpty()) {
            warnings.add(schedules.size() + " timetable assignment" + (schedules.size() > 1 ? "s" : ""));
        }

        boolean hasActiveResponsibilities = !warnings.isEmpty();
        String message = hasActiveResponsibilities
                ? f.getName() + " is currently assigned as " + String.join(" and has ", warnings) + "."
                : "No active assignments found.";

        return java.util.Map.of(
                "teacherId", id,
                "teacherName", f.getName(),
                "classTeacherOf", ct != null ? ct : "",
                "assignmentCount", assignments.size(),
                "scheduleCount", schedules.size(),
                "hasActiveResponsibilities", hasActiveResponsibilities,
                "warningMessage", message
        );
    }

    @Override
    public java.util.Map<String, String> getAllClassTeachers() {
        java.util.Map<String, String> result = new java.util.HashMap<>();
        repository.findAll().forEach(f -> {
            if (f.getClassTeacherOf() != null && !f.getClassTeacherOf().isBlank() && "Active".equalsIgnoreCase(f.getStatus())) {
                result.put(f.getClassTeacherOf().trim(), f.getName());
            }
        });
        return result;
    }

    @Override
    public String uploadPhoto(Long id, MultipartFile file) {
        Faculty f = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found with id: " + id));

        String savedPath = saveFile(file, "faculty_" + id);
        f.setPhotoUrl(savedPath);
        repository.save(f);
        return savedPath;
    }

    @Override
    public String uploadGeneralPhoto(MultipartFile file) {
        return saveFile(file, "faculty_temp_" + UUID.randomUUID().toString().substring(0, 8));
    }

    private String saveFile(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File cannot be empty");
        }

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }
        if (!ext.matches("\\.(jpg|jpeg|png|webp|gif)")) {
            ext = ".jpg";
        }

        try {
            Path uploadDir = Paths.get("uploads", "faculty").toAbsolutePath().normalize();
            File dir = uploadDir.toFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String filename = prefix + "_" + System.currentTimeMillis() + ext;
            Path targetLocation = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return "/faculty-service/uploads/faculty/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Could not store file: " + e.getMessage(), e);
        }
    }
}
