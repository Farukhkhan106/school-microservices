package com.successacademy.staffservice.service;

import com.successacademy.staffservice.dto.DepartmentRequest;
import com.successacademy.staffservice.dto.DepartmentResponse;
import com.successacademy.staffservice.model.Department;
import com.successacademy.staffservice.repository.DepartmentRepository;
import com.successacademy.staffservice.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final StaffRepository staffRepository;
    private final AuditLogService auditLogService;

    @Override
    public List<DepartmentResponse> getAllDepartments(boolean activeOnly) {
        List<Department> list = activeOnly
                ? departmentRepository.findByStatusIgnoreCase("ACTIVE")
                : departmentRepository.findAll();

        return list.stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public DepartmentResponse getDepartmentById(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found with id: " + id));
        return mapToResponse(dept);
    }

    @Override
    public DepartmentResponse createDepartment(DepartmentRequest req, Long actorUserId) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department name is required.");
        }

        String code = (req.getCode() != null && !req.getCode().isBlank())
                ? req.getCode().trim().toUpperCase()
                : generateCodeFromName(req.getName());

        if (departmentRepository.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department code already exists: " + code);
        }
        if (departmentRepository.existsByNameIgnoreCase(req.getName().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department name already exists: " + req.getName());
        }

        Department dept = Department.builder()
                .code(code)
                .name(req.getName().trim())
                .description(req.getDescription())
                .status((req.getStatus() != null && !req.getStatus().isBlank()) ? req.getStatus().toUpperCase() : "ACTIVE")
                .build();

        Department saved = departmentRepository.save(dept);
        auditLogService.log(actorUserId, null, "DEPARTMENT_CREATED", null, saved.getCode(), "Created department " + saved.getName());
        return mapToResponse(saved);
    }

    @Override
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest req, Long actorUserId) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found with id: " + id));

        if (req.getName() != null && !req.getName().isBlank() && !dept.getName().equalsIgnoreCase(req.getName().trim())) {
            if (departmentRepository.existsByNameIgnoreCase(req.getName().trim())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Department name already exists: " + req.getName());
            }
            dept.setName(req.getName().trim());
        }

        if (req.getDescription() != null) {
            dept.setDescription(req.getDescription());
        }

        Department updated = departmentRepository.save(dept);
        auditLogService.log(actorUserId, null, "DEPARTMENT_UPDATED", null, updated.getCode(), "Updated department " + updated.getName());
        return mapToResponse(updated);
    }

    @Override
    public DepartmentResponse updateDepartmentStatus(Long id, String status, Long actorUserId) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found with id: " + id));

        String newStatus = status.trim().toUpperCase();
        if ("INACTIVE".equals(newStatus)) {
            long count = staffRepository.countByDepartmentId(id);
            if (count > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot deactivate department with " + count + " assigned staff member(s). Reassign staff first.");
            }
        }

        String oldStatus = dept.getStatus();
        dept.setStatus(newStatus);
        Department updated = departmentRepository.save(dept);
        auditLogService.log(actorUserId, null, "DEPARTMENT_STATUS_CHANGED", oldStatus, newStatus, "Status of department " + dept.getName());
        return mapToResponse(updated);
    }

    private DepartmentResponse mapToResponse(Department d) {
        long count = staffRepository.countByDepartmentId(d.getId());
        return DepartmentResponse.builder()
                .id(d.getId())
                .code(d.getCode())
                .name(d.getName())
                .description(d.getDescription())
                .status(d.getStatus())
                .staffCount(count)
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    private String generateCodeFromName(String name) {
        return name.trim().toUpperCase()
                .replaceAll("[^A-Z0-9]", "_")
                .replaceAll("_+", "_");
    }
}
