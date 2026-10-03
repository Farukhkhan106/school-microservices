package com.successacademy.staffservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffSummaryResponse {

    private Long id;
    private String staffCode;
    private String fullName;
    private String photoUrl;
    private String departmentName;
    private String designationName;
    private String phone;
    private String status;
    private String systemAccessStatus;
    private String accessProfile;
}
