package com.successacademy.studentservice.service;

import com.successacademy.studentservice.client.AuthServiceClient;
import com.successacademy.studentservice.dto.StudentProfileResponse;
import com.successacademy.studentservice.dto.StudentRequest;
import com.successacademy.studentservice.dto.StudentResponse;
import com.successacademy.studentservice.model.Student;
import com.successacademy.studentservice.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final AuthServiceClient authServiceClient;

    // ─────────────────────────────────────────────────────────────
    //  CREATE — also auto-creates login account in auth-service
    // ─────────────────────────────────────────────────────────────
    @Override
    public StudentResponse addStudent(StudentRequest r) {
        Student student = mapToEntity(r);
        Student saved = studentRepository.save(student);

        // Auto-create login account (async-friendly — won't fail the student creation)
        authServiceClient.createStudentUser(
            saved.getId(),
            saved.getFirstName(),
            saved.getLastName(),
            saved.getEmail(),
            saved.getAdmissionNo()
        );

        return mapToResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────
    //  UPDATE
    // ─────────────────────────────────────────────────────────────
    @Override
    public StudentResponse updateStudent(Long id, StudentRequest r) {
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));

        // BASIC
        s.setFirstName(r.getFirstName());
        s.setLastName(r.getLastName());
        s.setEmail(r.getEmail());
        s.setPhone(r.getPhone());
        s.setGender(r.getGender());
        s.setDateOfBirth(r.getDateOfBirth());
        s.setPhotoUrl(r.getPhotoUrl());

        // ACADEMIC
        s.setAdmissionNo(r.getAdmissionNo());
        s.setRollNo(r.getRollNo());
        s.setAdmissionDate(r.getAdmissionDate());
        s.setStatus(r.getStatus());
        s.setStudentClass(r.getStudentClass());
        s.setSection(r.getSection());

        // ADDRESS
        s.setAddress(r.getAddress());
        s.setCity(r.getCity());
        s.setState(r.getState());
        s.setPincode(r.getPincode());

        // PARENTS
        s.setFatherName(r.getFatherName());
        s.setFatherPhone(r.getFatherPhone());
        s.setFatherOccupation(r.getFatherOccupation());
        s.setMotherName(r.getMotherName());
        s.setMotherPhone(r.getMotherPhone());
        s.setMotherOccupation(r.getMotherOccupation());

        // GUARDIAN
        s.setGuardianName(r.getGuardianName());
        s.setGuardianPhone(r.getGuardianPhone());
        s.setGuardianRelation(r.getGuardianRelation());

        return mapToResponse(studentRepository.save(s));
    }

    // ─────────────────────────────────────────────────────────────
    //  READ — full profile (student portal)
    // ─────────────────────────────────────────────────────────────
    @Override
    public StudentProfileResponse getMyProfile(Long id) {
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));

        return StudentProfileResponse.builder()
                .id(s.getId())
                .firstName(s.getFirstName())
                .lastName(s.getLastName())
                .admissionNo(s.getAdmissionNo())
                .status(s.getStatus())
                .photoUrl(s.getPhotoUrl())
                // academic
                .studentClass(s.getStudentClass())
                .section(s.getSection())
                .rollNo(s.getRollNo())
                .admissionDate(s.getAdmissionDate())
                // personal
                .gender(s.getGender())
                .email(s.getEmail())
                .phone(s.getPhone())
                .dateOfBirth(s.getDateOfBirth())
                // address
                .address(s.getAddress())
                .city(s.getCity())
                .state(s.getState())
                .pincode(s.getPincode())
                // parents
                .fatherName(s.getFatherName())
                .fatherPhone(s.getFatherPhone())
                .fatherOccupation(s.getFatherOccupation())
                .motherName(s.getMotherName())
                .motherPhone(s.getMotherPhone())
                .motherOccupation(s.getMotherOccupation())
                // guardian
                .guardianName(s.getGuardianName())
                .guardianPhone(s.getGuardianPhone())
                .guardianRelation(s.getGuardianRelation())
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    //  READ — slim by id
    // ─────────────────────────────────────────────────────────────
    @Override
    public StudentResponse getStudentById(Long id) {
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        return mapToResponse(s);
    }

    // ─────────────────────────────────────────────────────────────
    //  READ — list all
    // ─────────────────────────────────────────────────────────────
    @Override
    public List<StudentResponse> getAllStudents() {
        return studentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────
    //  DELETE
    // ─────────────────────────────────────────────────────────────
    @Override
    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new RuntimeException("Student not found with id: " + id);
        }
        studentRepository.deleteById(id);
    }

    // ─────────────────────────────────────────────────────────────
    //  TOGGLE STATUS
    // ─────────────────────────────────────────────────────────────
    @Override
    public StudentResponse toggleStatus(Long id) {
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
        String current = s.getStatus() != null ? s.getStatus() : "Active";
        s.setStatus("Active".equals(current) ? "Inactive" : "Active");
        studentRepository.save(s);
        return mapToResponse(s);
    }

    // ─────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────

    private Student mapToEntity(StudentRequest r) {
        return Student.builder()
                // basic
                .firstName(r.getFirstName())
                .lastName(r.getLastName())
                .email(r.getEmail())
                .phone(r.getPhone())
                .gender(r.getGender())
                .dateOfBirth(r.getDateOfBirth())
                .photoUrl(r.getPhotoUrl())
                // academic
                .admissionNo(r.getAdmissionNo())
                .rollNo(r.getRollNo())
                .admissionDate(r.getAdmissionDate())
                .status(r.getStatus())
                .studentClass(r.getStudentClass())
                .section(r.getSection())
                // address
                .address(r.getAddress())
                .city(r.getCity())
                .state(r.getState())
                .pincode(r.getPincode())
                // parents
                .fatherName(r.getFatherName())
                .fatherPhone(r.getFatherPhone())
                .fatherOccupation(r.getFatherOccupation())
                .motherName(r.getMotherName())
                .motherPhone(r.getMotherPhone())
                .motherOccupation(r.getMotherOccupation())
                // guardian
                .guardianName(r.getGuardianName())
                .guardianPhone(r.getGuardianPhone())
                .guardianRelation(r.getGuardianRelation())
                .build();
    }

    private StudentResponse mapToResponse(Student s) {
        return StudentResponse.builder()
                .id(s.getId())
                .admissionNo(s.getAdmissionNo())
                .firstName(s.getFirstName())
                .lastName(s.getLastName())
                .email(s.getEmail())
                .phone(s.getPhone())
                .gender(s.getGender())
                .studentClass(s.getStudentClass())
                .section(s.getSection())
                .rollNo(s.getRollNo())
                .status(s.getStatus())
                .photoUrl(s.getPhotoUrl())
                .fatherName(s.getFatherName())
                .fatherPhone(s.getFatherPhone())
                .build();
    }

    @Override
    public String uploadPhoto(Long id, MultipartFile file) {
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));

        String savedPath = saveFile(file, "student_" + id);
        s.setPhotoUrl(savedPath);
        studentRepository.save(s);
        return savedPath;
    }

    @Override
    public String uploadGeneralPhoto(MultipartFile file) {
        return saveFile(file, "student_temp_" + UUID.randomUUID().toString().substring(0, 8));
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
            Path uploadDir = Paths.get("uploads", "students").toAbsolutePath().normalize();
            File dir = uploadDir.toFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String filename = prefix + "_" + System.currentTimeMillis() + ext;
            Path targetLocation = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return "/student-service/uploads/students/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Could not store file: " + e.getMessage(), e);
        }
    }
}
