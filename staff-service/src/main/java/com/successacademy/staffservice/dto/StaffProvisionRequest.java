package com.successacademy.staffservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffProvisionRequest {

    private String username;
    private String password;
    private String accessProfile; // STAFF_BASIC, RECEPTION, ACCOUNTING, TRANSPORT, LIBRARY, SECURITY, HOUSEKEEPING, STAFF_MANAGER
}
