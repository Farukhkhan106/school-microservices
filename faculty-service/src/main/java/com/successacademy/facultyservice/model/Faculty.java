package com.successacademy.facultyservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "faculty", indexes = {
        @Index(name = "idx_faculty_department", columnList = "department"),
        @Index(name = "idx_faculty_status", columnList = "status"),
        @Index(name = "idx_faculty_user_id", columnList = "user_id")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Faculty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String email;
    private String phone;
    private String designation;       // e.g. "Senior Mathematics Teacher"
    private String qualification;     // e.g. "M.Sc., B.Ed."
    private int experience;           // years

    // Comma-separated: "Mathematics,Physics"
    private String subjects;

    private String classTeacherOf;    // e.g. "10-A" (nullable)

    private String photoUrl;

    private String status;            // Active | Inactive

    // Links to auth-service users.id (nullable)
    private Long userId;

    @Column(name = "faculty_code", unique = true)
    private String facultyCode;       // e.g. "FAC-0001"

    private String department;        // e.g. "Science", "Mathematics"

    private java.time.LocalDate joiningDate;

    private String employmentType;    // "FULL_TIME", "PART_TIME", "CONTRACT"

    private java.math.BigDecimal baseSalary;
}
