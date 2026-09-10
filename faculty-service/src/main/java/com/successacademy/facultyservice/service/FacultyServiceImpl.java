package com.successacademy.facultyservice.service;

import com.successacademy.facultyservice.client.AuthServiceClient;
import com.successacademy.facultyservice.dto.AssignmentRequest;
import com.successacademy.facultyservice.dto.AssignmentResponse;
import com.successacademy.facultyservice.dto.FacultyRequest;
import com.successacademy.facultyservice.dto.FacultyResponse;
import com.successacademy.facultyservice.dto.ScheduleRequest;
import com.successacademy.facultyservice.dto.ScheduleResponse;
import com.successacademy.facultyservice.dto.TeacherProfileResponse;
import com.successacademy.facultyservice.exception.ConflictException;
import com.successacademy.facultyservice.exception.NotFoundException;
import com.successacademy.facultyservice.model.ClassSchedule;
import com.successacademy.facultyservice.model.Faculty;
import com.successacademy.facultyservice.model.TeacherClassAssignment;
import com.successacademy.facultyservice.repository.ClassScheduleRepository;
import com.successacademy.facultyservice.repository.FacultyRepository;
import com.successacademy.facultyservice.repository.TeacherClassAssignmentRepository;
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

        return mapToResponse(repository.save(faculty));
    }

    @Override
    public void deleteFaculty(Long id) {
        if (!repository.existsById(id)) throw new RuntimeException("Faculty not found with id: " + id);
        repository.deleteById(id);
    }

    @Override
    public List<FacultyResponse> getAllFaculty() {
        return repository.findAll().stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(this::mapToResponse).toList();
    }

    @Override
    public List<FacultyResponse> getActiveFaculty() {
        return repository.findByStatusIgnoreCase("Active").stream()
                .map(this::mapToResponse).toList();
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

    @Override
    public TeacherProfileResponse getTeacherProfile(Long authUserId) {
        Faculty f = repository.findByUserId(authUserId)
                .orElseThrow(() -> new NotFoundException("No teacher profile is linked to this account"));
        List<AssignmentResponse> assignments = assignmentRepository
                .findByTeacherIdOrderById(f.getId()).stream().map(this::toAssignmentResponse).toList();
        List<ScheduleResponse> schedule = scheduleRepository
                .findByTeacherIdOrderByDayOfWeekAscPeriodNoAsc(f.getId()).stream().map(this::toScheduleResponse).toList();

        Set<String> allowed = new LinkedHashSet<>();
        if (f.getClassTeacherOf() != null && !f.getClassTeacherOf().isBlank()) allowed.add(f.getClassTeacherOf());
        assignments.stream()
                .filter(a -> "Active".equalsIgnoreCase(a.getStatus()))
                .forEach(a -> allowed.add(a.getStudentClass() + "-" + a.getSection()));

        return TeacherProfileResponse.builder()
                .faculty(mapToResponse(f))
                .assignments(assignments)
                .schedule(schedule)
                .allowedClasses(new ArrayList<>(allowed))
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
        Faculty f = repository.findByUserId(authUserId)
                .orElseThrow(() -> new NotFoundException("No teacher profile is linked to this account"));
        Set<String> allowed = new LinkedHashSet<>();
        if (f.getClassTeacherOf() != null && !f.getClassTeacherOf().isBlank()) allowed.add(f.getClassTeacherOf());
        assignmentRepository.findByTeacherIdOrderById(f.getId()).stream()
                .filter(a -> "Active".equalsIgnoreCase(a.getStatus()))
                .forEach(a -> allowed.add(a.getStudentClass() + "-" + a.getSection()));
        return new ArrayList<>(allowed);
    }

    @Override
    public boolean hasAnyAccess(Long authUserId, String studentClass, String section) {
        Faculty f = repository.findByUserId(authUserId).orElse(null);
        if (f == null) return false;
        String cs = studentClass + "-" + section;
        if (cs.equalsIgnoreCase(f.getClassTeacherOf())) return true;
        return !assignmentRepository
                .findByTeacherIdAndStudentClassAndSectionIgnoreCase(f.getId(), studentClass, section).isEmpty();
    }

    @Override
    public boolean hasClassTeacherAccess(Long authUserId, String studentClass, String section) {
        return repository.findByUserId(authUserId)
                .map(f -> (studentClass + "-" + section).equalsIgnoreCase(f.getClassTeacherOf()))
                .orElse(false);
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

        return FacultyResponse.builder()
                .id(f.getId()).name(f.getName()).email(f.getEmail()).phone(f.getPhone())
                .designation(f.getDesignation()).qualification(f.getQualification())
                .experience(f.getExperience()).subjects(subjectList)
                .classTeacherOf(f.getClassTeacherOf()).photoUrl(f.getPhotoUrl())
                .status(f.getStatus()).userId(f.getUserId())
                .build();
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
