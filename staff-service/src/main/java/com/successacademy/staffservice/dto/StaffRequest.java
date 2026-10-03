package com.successacademy.staffservice.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffRequest {

    private String firstName;
    private String middleName;
    private String lastName;
    private String photoUrl;

    private String gender;
    private LocalDate dateOfBirth;

    // Optional contact
    private String phone;
    private String alternatePhone;
    private String email;

    // Address
    private String address;
    private String city;
    private String state;
    private String pincode;

    // Emergency Contact
    private String emergencyContactName;
    private String emergencyContactRelation;
    private String emergencyContactPhone;

    // Organization
    private Long departmentId;
    private Long designationId;
    private String category;
    private String designation;
    private String department;
    private String employeeId;

    // Employment
    private String employmentType; // FULL_TIME, PART_TIME, CONTRACT, TEMPORARY, OUTSOURCED, INTERN
    private LocalDate joiningDate;
    private LocalDate lastWorkingDate;
    private Long reportingManagerStaffId;
    private String status; // ACTIVE, ON_NOTICE, SUSPENDED, RESIGNED, TERMINATED, RETIRED

    // Initial Salary configuration (optional)
    private String salaryType; // MONTHLY, DAILY
    private java.math.BigDecimal baseSalary;
    private java.math.BigDecimal transportAllowance;
    private java.math.BigDecimal otherAllowance;
    private java.math.BigDecimal fixedDeduction;

    // Initial system access provisioning (optional)
    private Boolean createSystemAccess;
    private String username;
    private String password;
    private String accessProfile; // STAFF_BASIC, RECEPTION, ACCOUNTING, TRANSPORT, LIBRARY, SECURITY, HOUSEKEEPING, STAFF_MANAGER
}
