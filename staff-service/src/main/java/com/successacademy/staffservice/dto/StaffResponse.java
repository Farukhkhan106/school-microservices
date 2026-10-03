package com.successacademy.staffservice.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffResponse {

    private Long id;
    private String staffCode;
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;
    private String photoUrl;

    private String gender;
    private LocalDate dateOfBirth;

    private String phone;
    private String alternatePhone;
    private String email;

    private String address;
    private String city;
    private String state;
    private String pincode;

    private String emergencyContactName;
    private String emergencyContactRelation;
    private String emergencyContactPhone;

    private Long departmentId;
    private String departmentName;
    private String departmentCode;
    private String department;

    private Long designationId;
    private String designationName;
    private String designationCode;
    private String designation;

    private String category;

    // Salary summary if configured
    private String salaryType;
    private java.math.BigDecimal baseSalary;
    private java.math.BigDecimal grossSalary;
    private java.math.BigDecimal netSalary;

    private String employmentType;
    private LocalDate joiningDate;
    private LocalDate lastWorkingDate;

    private Long reportingManagerStaffId;
    private String reportingManagerName;

    private String status;
    private String systemAccessStatus;
    private Long userId;
    private String username;
    private String accessProfile;

    // Shift info if assigned
    private Long currentShiftId;
    private String currentShiftName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
