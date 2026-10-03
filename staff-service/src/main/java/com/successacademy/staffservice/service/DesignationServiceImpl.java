package com.successacademy.staffservice.service;

import com.successacademy.staffservice.dto.DesignationRequest;
import com.successacademy.staffservice.dto.DesignationResponse;
import com.successacademy.staffservice.model.Department;
import com.successacademy.staffservice.model.Designation;
import com.successacademy.staffservice.repository.DepartmentRepository;
import com.successacademy.staffservice.repository.DesignationRepository;
import com.successacademy.staffservice.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DesignationServiceImpl implements DesignationService {

    private final DesignationRepository designationRepository;
    private final DepartmentRepository departmentRepository;
    private final StaffRepository staffRepository;
    private final AuditLogService auditLogService;

    @Override
    public List<DesignationResponse> getAllDesignations(Long departmentId, boolean activeOnly) {
        List<Designation> list;
        if (departmentId != null) {
            list = activeOnly
                    ? designationRepository.findByDepartmentIdAndStatusIgnoreCase(departmentId, "ACTIVE")
                    : designationRepository.findByDepartmentId(departmentId);
        } else {
            list = activeOnly
                    ? designationRepository.findByStatusIgnoreCase("ACTIVE")
                    : designationRepository.findAll();
        }

        return list.stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public DesignationResponse getDesignationById(Long id) {
        Designation d = designationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designation not found with id: " + id));
        return mapToResponse(d);
    }

    @Override
    public DesignationResponse createDesignation(DesignationRequest req, Long actorUserId) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Designation name is required.");
        }
        if (req.getDepartmentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department ID is required.");
        }

        Department dept = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specified department does not exist."));

        String code = (req.getCode() != null && !req.getCode().isBlank())
                ? req.getCode().trim().toUpperCase()
                : generateCodeFromName(req.getName());

        if (designationRepository.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Designation code already exists: " + code);
        }

        if (designationRepository.existsByNameIgnoreCaseAndDepartmentId(req.getName().trim(), req.getDepartmentId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Designation already exists in department: " + req.getName());
        }

        Designation desig = Designation.builder()
                .departmentId(dept.getId())
                .code(code)
                .name(req.getName().trim())
                .description(req.getDescription())
                .status((req.getStatus() != null && !req.getStatus().isBlank()) ? req.getStatus().toUpperCase() : "ACTIVE")
                .build();

        Designation saved = designationRepository.save(desig);
        auditLogService.log(actorUserId, null, "DESIGNATION_CREATED", null, saved.getCode(), "Created designation " + saved.getName());
        return mapToResponse(saved);
    }

    @Override
    public DesignationResponse updateDesignation(Long id, DesignationRequest req, Long actorUserId) {
        Designation desig = designationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designation not found with id: " + id));

        if (req.getDepartmentId() != null && !desig.getDepartmentId().equals(req.getDepartmentId())) {
            departmentRepository.findById(req.getDepartmentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specified department does not exist."));
            desig.setDepartmentId(req.getDepartmentId());
        }

        if (req.getName() != null && !req.getName().isBlank() && !desig.getName().equalsIgnoreCase(req.getName().trim())) {
            if (designationRepository.existsByNameIgnoreCaseAndDepartmentId(req.getName().trim(), desig.getDepartmentId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Designation already exists in this department: " + req.getName());
            }
            desig.setName(req.getName().trim());
        }

        if (req.getDescription() != null) {
            desig.setDescription(req.getDescription());
        }

        Designation updated = designationRepository.save(desig);
        auditLogService.log(actorUserId, null, "DESIGNATION_UPDATED", null, updated.getCode(), "Updated designation " + updated.getName());
        return mapToResponse(updated);
    }

    @Override
    public DesignationResponse updateDesignationStatus(Long id, String status, Long actorUserId) {
        Designation desig = designationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designation not found with id: " + id));

        String newStatus = status.trim().toUpperCase();
        if ("INACTIVE".equals(newStatus)) {
            long count = staffRepository.countByDesignationId(id);
            if (count > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot deactivate designation with " + count + " assigned staff member(s). Reassign staff first.");
            }
        }

        String oldStatus = desig.getStatus();
        desig.setStatus(newStatus);
        Designation updated = designationRepository.save(desig);
        auditLogService.log(actorUserId, null, "DESIGNATION_STATUS_CHANGED", oldStatus, newStatus, "Status of designation " + desig.getName());
        return mapToResponse(updated);
    }

    private DesignationResponse mapToResponse(Designation d) {
        String deptName = departmentRepository.findById(d.getDepartmentId())
                .map(Department::getName)
                .orElse("Unknown Department");

        long count = staffRepository.countByDesignationId(d.getId());

        return DesignationResponse.builder()
                .id(d.getId())
                .departmentId(d.getDepartmentId())
                .departmentName(deptName)
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
