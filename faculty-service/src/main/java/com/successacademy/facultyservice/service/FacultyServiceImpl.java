package com.successacademy.facultyservice.service;

import com.successacademy.facultyservice.client.AuthServiceClient;
import com.successacademy.facultyservice.dto.FacultyRequest;
import com.successacademy.facultyservice.dto.FacultyResponse;
import com.successacademy.facultyservice.model.Faculty;
import com.successacademy.facultyservice.repository.FacultyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FacultyServiceImpl implements FacultyService {

    private final FacultyRepository repository;
    private final AuthServiceClient authServiceClient;
    private final com.successacademy.facultyservice.repository.FacultyMonthlySalaryRepository monthlySalaryRepository;

    @Override
    public FacultyResponse addFaculty(FacultyRequest request) {
        Faculty faculty = mapToEntity(request);
        Faculty saved = repository.save(faculty);

        if (saved.getFacultyCode() == null || saved.getFacultyCode().isBlank()) {
            saved.setFacultyCode(String.format("FAC-%04d", saved.getId()));
            saved = repository.save(saved);
        }

        // Auto-create login account in auth-service if needed
        try {
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
        } catch (Exception ignored) {}

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
        if (request.getFacultyCode() != null && !request.getFacultyCode().isBlank()) {
            faculty.setFacultyCode(request.getFacultyCode().trim());
        }
        if (request.getDepartment() != null) faculty.setDepartment(request.getDepartment().trim());
        if (request.getJoiningDate() != null) faculty.setJoiningDate(request.getJoiningDate());
        if (request.getEmploymentType() != null) faculty.setEmploymentType(request.getEmploymentType().trim());
        if (request.getBaseSalary() != null) faculty.setBaseSalary(request.getBaseSalary());

        return mapToResponse(repository.save(faculty));
    }

    @Override
    public void deleteFaculty(Long id) {
        if (!repository.existsById(id)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Faculty not found with id: " + id);
        }
        if (!monthlySalaryRepository.findByFacultyIdOrderByYearDescMonthDesc(id).isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                    "Cannot delete faculty with existing payroll records. Please deactivate the faculty status instead to preserve historical records.");
        }
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
                .map(f -> {
                    FacultyResponse res = mapToResponse(f);
                    res.setBaseSalary(null);
                    res.setPhone(null);
                    if (res.getEmail() != null && res.getEmail().contains("@")) {
                        String[] parts = res.getEmail().split("@");
                        String u = parts[0];
                        String masked = (u.length() <= 2) ? u.charAt(0) + "***" : u.substring(0, 2) + "***";
                        res.setEmail(masked + "@" + parts[1]);
                    }
                    return res;
                }).toList();
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

    // ─── HELPERS ────────────────────────────────────────────────

    private Faculty mapToEntity(FacultyRequest r) {
        return Faculty.builder()
                .name(r.getName()).email(r.getEmail()).phone(r.getPhone())
                .designation(r.getDesignation()).qualification(r.getQualification())
                .experience(r.getExperience()).subjects(r.getSubjects())
                .classTeacherOf(r.getClassTeacherOf()).photoUrl(r.getPhotoUrl())
                .status(r.getStatus() != null ? r.getStatus() : "Active")
                .userId(r.getUserId())
                .facultyCode(r.getFacultyCode() != null && !r.getFacultyCode().isBlank() ? r.getFacultyCode().trim() : null)
                .department(r.getDepartment() != null ? r.getDepartment().trim() : "Academic")
                .joiningDate(r.getJoiningDate() != null ? r.getJoiningDate() : java.time.LocalDate.now())
                .employmentType(r.getEmploymentType() != null ? r.getEmploymentType().trim() : "FULL_TIME")
                .baseSalary(r.getBaseSalary() != null ? r.getBaseSalary() : java.math.BigDecimal.valueOf(26000.00))
                .build();
    }

    private FacultyResponse mapToResponse(Faculty f) {
        List<String> subjectList = (f.getSubjects() != null && !f.getSubjects().isBlank())
                ? Arrays.stream(f.getSubjects().split(",")).map(String::trim).toList()
                : List.of();

        String code = f.getFacultyCode();
        if (code == null || code.isBlank()) {
            code = String.format("FAC-%04d", f.getId());
        }

        return FacultyResponse.builder()
                .id(f.getId()).name(f.getName()).email(f.getEmail()).phone(f.getPhone())
                .designation(f.getDesignation()).qualification(f.getQualification())
                .experience(f.getExperience()).subjects(subjectList)
                .classTeacherOf(f.getClassTeacherOf()).photoUrl(f.getPhotoUrl())
                .status(f.getStatus()).userId(f.getUserId())
                .facultyCode(code)
                .department(f.getDepartment() != null ? f.getDepartment() : "Academic")
                .joiningDate(f.getJoiningDate() != null ? f.getJoiningDate() : java.time.LocalDate.of(2024, 1, 1))
                .employmentType(f.getEmploymentType() != null ? f.getEmploymentType() : "FULL_TIME")
                .baseSalary(f.getBaseSalary() != null ? f.getBaseSalary() : java.math.BigDecimal.valueOf(26000.00))
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
