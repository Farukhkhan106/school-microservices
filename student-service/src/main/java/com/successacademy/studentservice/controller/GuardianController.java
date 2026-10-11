package com.successacademy.studentservice.controller;

import com.successacademy.studentservice.dto.*;
import com.successacademy.studentservice.model.Student;
import com.successacademy.studentservice.repository.StudentGuardianRepository;
import com.successacademy.studentservice.repository.StudentRepository;
import com.successacademy.studentservice.security.SecurityContextUtil;
import com.successacademy.studentservice.security.TenantContext;
import com.successacademy.studentservice.service.GuardianService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/guardians")
@RequiredArgsConstructor
@Slf4j
public class GuardianController {

    private final GuardianService guardianService;
    private final SecurityContextUtil securityContextUtil;
    private final StudentGuardianRepository studentGuardianRepository;
    private final StudentRepository studentRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuardianResponse createGuardian(@Valid @RequestBody GuardianRequest request, HttpServletRequest httpRequest) {
        securityContextUtil.requireAdmin(httpRequest);
        return guardianService.createGuardian(request);
    }

    @PutMapping("/{id}")
    public GuardianResponse updateGuardian(@PathVariable Long id,
                                            @Valid @RequestBody GuardianRequest request,
                                            HttpServletRequest httpRequest) {
        securityContextUtil.requireAdmin(httpRequest);
        return guardianService.updateGuardian(id, request);
    }

    @GetMapping("/{id}")
    public GuardianResponse getGuardian(@PathVariable Long id, HttpServletRequest httpRequest) {
        enforceGuardianAccess(id, httpRequest);
        return guardianService.getGuardian(id);
    }

    @GetMapping
    public Page<GuardianResponse> listGuardians(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            HttpServletRequest httpRequest) {
        securityContextUtil.requireAdmin(httpRequest);

        String[] sortParts = sort.split(",");
        String sortProp = sortParts[0];
        Sort.Direction direction = (sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1]))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(direction, sortProp));
        return guardianService.listGuardians(search, pageable);
    }

    @PostMapping("/students/{studentId}/link")
    public StudentGuardianDto linkGuardianToStudent(
            @PathVariable Long studentId,
            @Valid @RequestBody StudentGuardianLinkRequest request,
            HttpServletRequest httpRequest) {
        securityContextUtil.requireAdmin(httpRequest);
        return guardianService.linkGuardianToStudent(studentId, request);
    }

    @DeleteMapping("/students/{studentId}/unlink/{guardianId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlinkGuardianFromStudent(
            @PathVariable Long studentId,
            @PathVariable Long guardianId,
            HttpServletRequest httpRequest) {
        securityContextUtil.requireAdmin(httpRequest);
        guardianService.unlinkGuardianFromStudent(studentId, guardianId);
    }

    @GetMapping("/students/{studentId}")
    public List<StudentGuardianDto> getGuardiansForStudent(
            @PathVariable Long studentId,
            HttpServletRequest httpRequest) {
        enforceStudentAccess(studentId, httpRequest);
        return guardianService.getGuardiansForStudent(studentId);
    }

    /**
     * For logged-in guardian to view only their linked wards
     */
    @GetMapping("/me/students")
    public List<StudentResponse> getMyWards(HttpServletRequest httpRequest) {
        securityContextUtil.requireAuthenticated(httpRequest);
        Long guardianId = securityContextUtil.getCurrentGuardianId(httpRequest);
        if (guardianId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Not a guardian user");
        }
        String tenantId = TenantContext.getTenantId();
        List<Long> studentIds = studentGuardianRepository.findByGuardianIdAndTenantId(guardianId, tenantId).stream()
                .map(sg -> sg.getStudentId())
                .collect(Collectors.toList());

        return studentRepository.findAllById(studentIds).stream()
                .filter(s -> tenantId.equals(s.getTenantId()))
                .map((Student s) -> StudentResponse.builder()
                        .id(s.getId())
                        .firstName(s.getFirstName())
                        .lastName(s.getLastName())
                        .admissionNo(s.getAdmissionNo())
                        .rollNo(s.getRollNo())
                        .studentClass(s.getStudentClass())
                        .section(s.getSection())
                        .email(s.getEmail())
                        .phone(s.getPhone())
                        .gender(s.getGender())
                        .dob(s.getDateOfBirth())
                        .address(s.getAddress())
                        .status(s.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    private void enforceGuardianAccess(Long guardianId, HttpServletRequest httpRequest) {
        if (securityContextUtil.isAdmin(httpRequest)) return;
        Long currentGuardianId = securityContextUtil.getCurrentGuardianId(httpRequest);
        if (currentGuardianId == null || !currentGuardianId.equals(guardianId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You can only view your own guardian profile");
        }
    }

    private void enforceStudentAccess(Long studentId, HttpServletRequest httpRequest) {
        if (securityContextUtil.isAdmin(httpRequest)) return;
        String role = securityContextUtil.getCurrentRole(httpRequest);
        if ("GUARDIAN".equalsIgnoreCase(role)) {
            Long guardianId = securityContextUtil.getCurrentGuardianId(httpRequest);
            if (guardianId == null || !guardianService.isGuardianLinkedToStudent(guardianId, studentId, TenantContext.getTenantId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Guardian is not linked to this student");
            }
            return;
        }
        if ("STUDENT".equalsIgnoreCase(role)) {
            Long currentStudentId = securityContextUtil.getCurrentStudentId(httpRequest);
            if (currentStudentId == null || !currentStudentId.equals(studentId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You can only view your own records");
            }
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }
}
