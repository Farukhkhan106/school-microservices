package com.successacademy.studentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionConversionRequest {
    private Long academicSessionId;
    private String studentClass;
    private String section;
    private String rollNumber;
    private Boolean createStudentUserAccount;
    private Boolean createGuardianUserAccount;
}
