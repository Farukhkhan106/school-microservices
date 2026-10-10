package com.successacademy.facultyservice.controller;

import com.successacademy.facultyservice.dto.FacultyRequest;
import com.successacademy.facultyservice.dto.FacultyResponse;
import com.successacademy.facultyservice.model.ClassSchedule;
import com.successacademy.facultyservice.model.Faculty;
import com.successacademy.facultyservice.model.TeachingAssignment;
import com.successacademy.facultyservice.repository.ClassScheduleRepository;
import com.successacademy.facultyservice.repository.FacultyRepository;
import com.successacademy.facultyservice.repository.TeachingAssignmentRepository;
import com.successacademy.facultyservice.service.FacultyService;
import com.successacademy.facultyservice.security.UserContext;
import com.successacademy.facultyservice.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/faculty")
@RequiredArgsConstructor
public class FacultyController {

    private final FacultyService service;
    private final FacultyRepository facultyRepository;
    private final TeachingAssignmentRepository assignmentRepository;
    private final ClassScheduleRepository scheduleRepository;
    private final com.successacademy.facultyservice.repository.TeacherSubstituteRepository substituteRepository;

    // ── ADMIN CRUD ──────────────────────────────────────────────

    @PostMapping({"", "/add"})
    @ResponseStatus(HttpStatus.CREATED)
    public FacultyResponse add(@RequestBody FacultyRequest request, HttpServletRequest httpRequest) {
        UserContext.requireRole(httpRequest, "ADMIN");
        return service.addFaculty(request);
    }

    @PutMapping("/{id}")
    public FacultyResponse update(@PathVariable Long id,
                                  @RequestBody FacultyRequest request,
                                  HttpServletRequest httpRequest) {
        UserContext.requireRole(httpRequest, "ADMIN");
        return service.updateFaculty(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        UserContext.requireRole(httpRequest, "ADMIN");
        service.deleteFaculty(id);
    }

    // ── ADMIN / INTERNAL READ ───────────────────────────────────

    @GetMapping({"", "/all"})
    public ResponseEntity<List<FacultyResponse>> all() {
        return ResponseEntity.ok(service.getAllFaculty());
    }

    @GetMapping("/code/{facultyCode}")
    public ResponseEntity<FacultyResponse> getByCode(@PathVariable String facultyCode) {
        return facultyRepository.findByFacultyCode(facultyCode)
                .map(f -> ResponseEntity.ok(service.getFacultyById(f.getId())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacultyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getFacultyById(id));
    }

    // Get faculty by linked auth userId (for teacher portal)
    @GetMapping("/user/{userId}")
    public ResponseEntity<FacultyResponse> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getFacultyByUserId(userId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<FacultyResponse>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(service.searchByName(keyword));
    }

    @GetMapping("/subject")
    public ResponseEntity<List<FacultyResponse>> bySubject(@RequestParam String subject) {
        return ResponseEntity.ok(service.filterBySubject(subject));
    }

    // ── PUBLIC (NO AUTH — whitelisted in gateway) ───────────────

    @GetMapping("/public")
    public ResponseEntity<List<FacultyResponse>> publicFaculty() {
        return ResponseEntity.ok(service.getActiveFaculty());
    }

    @PostMapping("/{id}/photo")
    public java.util.Map<String, String> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String url = service.uploadPhoto(id, file);
        return java.util.Map.of("photoUrl", url);
    }

    @PostMapping("/upload-photo")
    public java.util.Map<String, String> uploadGeneralPhoto(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String url = service.uploadGeneralPhoto(file);
        return java.util.Map.of("photoUrl", url);
    }

    // ── TEACHING ASSIGNMENTS ────────────────────────────────────

    @GetMapping("/assignments")
    public ResponseEntity<List<TeachingAssignment>> getAllAssignments(HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return ResponseEntity.ok(assignmentRepository.findAll());
    }

    @GetMapping("/assignments/teacher/{teacherId}")
    public ResponseEntity<List<TeachingAssignment>> getAssignmentsByTeacher(@PathVariable Long teacherId, HttpServletRequest request) {
        String role = UserContext.role(request);
        if (!role.isBlank()) {
            if (UserContext.isTeacher(request)) {
                Long authTeacherId = UserContext.teacherId(request);
                if (authTeacherId == null) {
                    Long authUserId = UserContext.userId(request);
                    if (authUserId != null) {
                        Faculty f = facultyRepository.findByUserId(authUserId).orElse(null);
                        if (f != null) authTeacherId = f.getId();
                    }
                }
                if (authTeacherId == null || !authTeacherId.equals(teacherId)) {
                    throw new ForbiddenException("Access denied: You can only view your own teaching assignments");
                }
            } else if (!UserContext.isAdmin(request)) {
                throw new ForbiddenException("Access denied: Insufficient privileges to view teaching assignments");
            }
        }
        return ResponseEntity.ok(assignmentRepository.findByTeacherId(teacherId));
    }

    @PostMapping("/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<TeachingAssignment> addAssignment(@RequestBody TeachingAssignment assignment, HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        if (assignment.getTeacherName() == null && assignment.getTeacherId() != null) {
            facultyRepository.findById(assignment.getTeacherId())
                    .ifPresent(f -> assignment.setTeacherName(f.getName()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentRepository.save(assignment));
    }

    @PutMapping("/assignments/{id}")
    public ResponseEntity<TeachingAssignment> updateAssignment(@PathVariable Long id,
                                                               @RequestBody TeachingAssignment req,
                                                               HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return assignmentRepository.findById(id).map(existing -> {
            existing.setTeacherId(req.getTeacherId());
            existing.setTeacherName(req.getTeacherName());
            existing.setStudentClass(req.getStudentClass());
            existing.setSection(req.getSection());
            existing.setSubject(req.getSubject());
            if (req.getStatus() != null) existing.setStatus(req.getStatus());
            return ResponseEntity.ok(assignmentRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/assignments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssignment(@PathVariable Long id, HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        assignmentRepository.deleteById(id);
    }

    // ── TIMETABLE / CLASS SCHEDULES ─────────────────────────────

    @GetMapping("/schedule")
    public ResponseEntity<List<ClassSchedule>> getAllSchedules() {
        return ResponseEntity.ok(scheduleRepository.findAll());
    }

    @GetMapping("/schedule/class")
    public ResponseEntity<List<ClassSchedule>> getSchedulesByClass(@RequestParam String studentClass,
                                                                   @RequestParam String section) {
        return ResponseEntity.ok(scheduleRepository.findByStudentClassAndSection(studentClass, section));
    }

    @GetMapping("/schedule/teacher/{teacherId}")
    public ResponseEntity<List<ClassSchedule>> getSchedulesByTeacher(@PathVariable Long teacherId) {
        return ResponseEntity.ok(scheduleRepository.findByTeacherId(teacherId));
    }

    @PostMapping("/schedule")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ClassSchedule> addSchedule(@RequestBody ClassSchedule schedule) {
        if (schedule.getTeacherName() == null && schedule.getTeacherId() != null) {
            facultyRepository.findById(schedule.getTeacherId())
                    .ifPresent(f -> schedule.setTeacherName(f.getName()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleRepository.save(schedule));
    }

    @PutMapping("/schedule/{id}")
    public ResponseEntity<ClassSchedule> updateSchedule(@PathVariable Long id,
                                                        @RequestBody ClassSchedule req) {
        return scheduleRepository.findById(id).map(existing -> {
            existing.setTeacherId(req.getTeacherId());
            existing.setTeacherName(req.getTeacherName());
            existing.setStudentClass(req.getStudentClass());
            existing.setSection(req.getSection());
            existing.setSubject(req.getSubject());
            existing.setDayOfWeek(req.getDayOfWeek());
            existing.setPeriodNo(req.getPeriodNo());
            existing.setStartTime(req.getStartTime());
            existing.setEndTime(req.getEndTime());
            return ResponseEntity.ok(scheduleRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/schedule/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSchedule(@PathVariable Long id) {
        scheduleRepository.deleteById(id);
    }

    // ── CLASS TEACHERS ──────────────────────────────────────────

    @GetMapping("/class-teachers")
    public ResponseEntity<Map<String, String>> getClassTeachers() {
        Map<String, String> map = new LinkedHashMap<>();
        facultyRepository.findAll().forEach(f -> {
            if (f.getClassTeacherOf() != null && !f.getClassTeacherOf().isBlank()) {
                map.put(f.getClassTeacherOf(), f.getName());
            }
        });
        return ResponseEntity.ok(map);
    }

    @PostMapping("/{id}/class-teacher")
    public ResponseEntity<FacultyResponse> assignClassTeacher(@PathVariable Long id,
                                                              @RequestParam String classTeacherOf,
                                                              @RequestParam(defaultValue = "false") boolean replace) {
        return facultyRepository.findById(id).map(faculty -> {
            if (replace) {
                facultyRepository.findAll().stream()
                        .filter(f -> classTeacherOf.equalsIgnoreCase(f.getClassTeacherOf()) && !f.getId().equals(id))
                        .forEach(f -> {
                            f.setClassTeacherOf(null);
                            facultyRepository.save(f);
                        });
            }
            faculty.setClassTeacherOf(classTeacherOf);
            facultyRepository.save(faculty);
            return ResponseEntity.ok(service.getFacultyById(id));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/check-deactivation")
    public ResponseEntity<Map<String, Object>> checkDeactivation(@PathVariable Long id) {
        return facultyRepository.findById(id).map(faculty -> {
            String classTeacherOf = faculty.getClassTeacherOf();
            int assignmentCount = assignmentRepository.findByTeacherId(id).size();
            int scheduleCount = scheduleRepository.findByTeacherId(id).size();
            boolean hasResponsibilities = (classTeacherOf != null && !classTeacherOf.isBlank())
                    || assignmentCount > 0 || scheduleCount > 0;

            StringBuilder warning = new StringBuilder();
            if (classTeacherOf != null && !classTeacherOf.isBlank()) {
                warning.append(faculty.getName()).append(" is currently assigned as Class Teacher of ").append(classTeacherOf).append(". ");
            }
            if (assignmentCount > 0) {
                warning.append("Has ").append(assignmentCount).append(" active class assignments. ");
            }
            if (scheduleCount > 0) {
                warning.append("Has ").append(scheduleCount).append(" timetable periods scheduled.");
            }

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("teacherId", id);
            res.put("teacherName", faculty.getName());
            res.put("classTeacherOf", classTeacherOf != null ? classTeacherOf : "");
            res.put("assignmentCount", assignmentCount);
            res.put("scheduleCount", scheduleCount);
            res.put("hasActiveResponsibilities", hasResponsibilities);
            res.put("warningMessage", warning.toString().trim());
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── TEACHER PORTAL ──────────────────────────────────────────

    @GetMapping("/teacher/me")
    public ResponseEntity<Map<String, Object>> getMyProfile(
            HttpServletRequest request,
            @RequestParam(required = false) Long teacherId) {
        UserContext.requireAuthenticated(request);
        Long resolvedId = null;
        if (UserContext.isAdmin(request) && teacherId != null) {
            resolvedId = teacherId;
        } else {
            Long authTeacherId = UserContext.teacherId(request);
            if (authTeacherId != null) {
                resolvedId = authTeacherId;
            } else {
                Long authUserId = UserContext.userId(request);
                if (authUserId != null) {
                    Faculty f = facultyRepository.findByUserId(authUserId).orElse(null);
                    if (f != null) resolvedId = f.getId();
                }
            }
        }
        if (resolvedId == null) {
            return ResponseEntity.notFound().build();
        }
        final Long fid = resolvedId;
        Faculty f = facultyRepository.findById(fid)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Faculty not found with id: " + fid));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("faculty", service.getFacultyById(fid));
        res.put("assignments", assignmentRepository.findByTeacherId(fid));
        res.put("schedule", scheduleRepository.findByTeacherId(fid));

        Set<String> classes = new LinkedHashSet<>();
        if (f.getClassTeacherOf() != null && !f.getClassTeacherOf().isBlank()) {
            classes.add(f.getClassTeacherOf());
        }
        assignmentRepository.findByTeacherId(fid).forEach(a -> classes.add(a.getStudentClass() + "-" + a.getSection()));
        res.put("allowedClasses", new ArrayList<>(classes));
        res.put("todaySubstitutions", substituteRepository.findBySubstituteTeacherIdAndDate(fid, java.time.LocalDate.now()));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/internal/allowed-classes")
    public List<String> getAllowedClasses(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long teacherId) {
        Long resolvedId = teacherId;
        if (resolvedId == null && userId != null) {
            Faculty f = facultyRepository.findByUserId(userId).orElse(null);
            if (f != null) resolvedId = f.getId();
        }
        if (resolvedId == null) {
            return Collections.emptyList();
        }
        Faculty f = facultyRepository.findById(resolvedId).orElse(null);
        if (f == null) {
            return Collections.emptyList();
        }
        Set<String> classes = new LinkedHashSet<>();
        if (f.getClassTeacherOf() != null && !f.getClassTeacherOf().isBlank()) {
            classes.add(f.getClassTeacherOf().trim());
        }
        assignmentRepository.findByTeacherId(resolvedId)
                .forEach(a -> classes.add((a.getStudentClass() + "-" + a.getSection()).trim()));
        return new ArrayList<>(classes);
    }

    @GetMapping("/teacher/access")
    public ResponseEntity<Map<String, Object>> getTeacherAccess(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam String studentClass,
            @RequestParam String section) {
        Long resolvedId = teacherId;
        if (resolvedId == null && userId != null) {
            Faculty f = facultyRepository.findByUserId(userId).orElse(null);
            if (f != null) resolvedId = f.getId();
        }
        boolean canView = false;
        boolean canMark = false;
        if (resolvedId != null) {
            Faculty f = facultyRepository.findById(resolvedId).orElse(null);
            String targetClass = studentClass + "-" + section;
            if (f != null && targetClass.equalsIgnoreCase(f.getClassTeacherOf())) {
                canView = true;
                canMark = true;
            } else if (resolvedId != null) {
                final Long checkFid = resolvedId;
                boolean hasClassAssn = !assignmentRepository.findByStudentClassAndSection(studentClass, section)
                        .stream().filter(a -> a.getTeacherId().equals(checkFid)).toList().isEmpty();
                boolean hasClassSched = !scheduleRepository.findByStudentClassAndSection(studentClass, section)
                        .stream().filter(s -> s.getTeacherId().equals(checkFid)).toList().isEmpty();
                if (hasClassAssn || hasClassSched) {
                    canView = true;
                }
            }
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("studentClass", studentClass);
        map.put("section", section);
        map.put("canView", canView);
        map.put("canMarkAttendance", canMark);
        return ResponseEntity.ok(map);
    }

    @GetMapping("/teacher/classes")
    public ResponseEntity<List<String>> getMyClasses(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(required = false) Long teacherId) {
        Long resolvedId = teacherId;
        if (resolvedId == null && userId != null) {
            Faculty f = facultyRepository.findByUserId(userId).orElse(null);
            if (f != null) resolvedId = f.getId();
        }
        if (resolvedId == null) {
            return ResponseEntity.ok(List.of());
        }
        Set<String> classes = new LinkedHashSet<>();
        assignmentRepository.findByTeacherId(resolvedId).forEach(a -> {
            classes.add(a.getStudentClass() + "-" + a.getSection());
        });
        scheduleRepository.findByTeacherId(resolvedId).forEach(s -> {
            classes.add(s.getStudentClass() + "-" + s.getSection());
        });
        return ResponseEntity.ok(new ArrayList<>(classes));
    }

    @GetMapping("/teacher/schedule")
    public ResponseEntity<List<ClassSchedule>> getMySchedule(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(required = false) Long teacherId) {
        Long resolvedId = teacherId;
        if (resolvedId == null && userId != null) {
            Faculty f = facultyRepository.findByUserId(userId).orElse(null);
            if (f != null) resolvedId = f.getId();
        }
        if (resolvedId == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(scheduleRepository.findByTeacherId(resolvedId));
    }

    // ── TEACHER ABSENCE & SUBSTITUTES ──────────────────────────

    @PostMapping("/substitutes")
    public ResponseEntity<com.successacademy.facultyservice.model.TeacherSubstitute> assignSubstitute(
            @RequestBody com.successacademy.facultyservice.model.TeacherSubstitute req) {
        if (req.getAbsentTeacherId() != null && req.getAbsentTeacherName() == null) {
            facultyRepository.findById(req.getAbsentTeacherId()).ifPresent(f -> req.setAbsentTeacherName(f.getName()));
        }
        if (req.getSubstituteTeacherId() != null && req.getSubstituteTeacherName() == null) {
            facultyRepository.findById(req.getSubstituteTeacherId()).ifPresent(f -> req.setSubstituteTeacherName(f.getName()));
        }
        if (req.getStatus() == null) {
            req.setStatus(req.getSubstituteTeacherId() != null ? "ASSIGNED" : "ABSENT");
        }
        return ResponseEntity.ok(substituteRepository.save(req));
    }

    @GetMapping("/substitutes")
    public ResponseEntity<List<com.successacademy.facultyservice.model.TeacherSubstitute>> getSubstitutes(
            @RequestParam(required = false) String date) {
        if (date != null && !date.isBlank()) {
            return ResponseEntity.ok(substituteRepository.findByDate(java.time.LocalDate.parse(date.trim())));
        }
        return ResponseEntity.ok(substituteRepository.findAll());
    }

    @GetMapping("/substitutes/teacher/{teacherId}")
    public ResponseEntity<List<com.successacademy.facultyservice.model.TeacherSubstitute>> getSubstitutesByTeacher(
            @PathVariable Long teacherId) {
        List<com.successacademy.facultyservice.model.TeacherSubstitute> list = new ArrayList<>(substituteRepository.findByAbsentTeacherId(teacherId));
        substituteRepository.findBySubstituteTeacherId(teacherId).forEach(s -> {
            if (!list.contains(s)) list.add(s);
        });
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/substitutes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelSubstitute(@PathVariable Long id) {
        substituteRepository.deleteById(id);
    }

    @GetMapping("/substitutes/overview")
    public ResponseEntity<Map<String, Object>> getAbsenceOverview(@RequestParam(required = false) String date) {
        java.time.LocalDate d = date != null && !date.isBlank() ? java.time.LocalDate.parse(date.trim()) : java.time.LocalDate.now();
        List<com.successacademy.facultyservice.model.TeacherSubstitute> subs = substituteRepository.findByDate(d);
        long absentCount = subs.stream().map(com.successacademy.facultyservice.model.TeacherSubstitute::getAbsentTeacherId).distinct().count();
        long covered = subs.stream().filter(s -> "ASSIGNED".equalsIgnoreCase(s.getStatus()) && s.getSubstituteTeacherId() != null).count();
        long uncovered = subs.stream().filter(s -> !"ASSIGNED".equalsIgnoreCase(s.getStatus()) || s.getSubstituteTeacherId() == null).count();

        Map<String, Object> res = new HashMap<>();
        res.put("date", d.toString());
        res.put("totalAbsentTeachers", absentCount);
        res.put("totalPeriodsCovered", covered);
        res.put("totalPeriodsUncovered", uncovered);
        res.put("substitutions", subs);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/teacher/substitutions")
    public ResponseEntity<List<com.successacademy.facultyservice.model.TeacherSubstitute>> getMySubstitutions(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(required = false) String date) {
        if (userId == null) return ResponseEntity.ok(List.of());
        Faculty f = facultyRepository.findByUserId(userId).orElse(null);
        if (f == null) return ResponseEntity.ok(List.of());
        if (date != null && !date.isBlank()) {
            return ResponseEntity.ok(substituteRepository.findBySubstituteTeacherIdAndDate(f.getId(), java.time.LocalDate.parse(date.trim())));
        }
        return ResponseEntity.ok(substituteRepository.findBySubstituteTeacherId(f.getId()));
    }
}
