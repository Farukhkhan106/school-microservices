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

    // ─── HELPERS ────────────────────────────────────────────────

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
