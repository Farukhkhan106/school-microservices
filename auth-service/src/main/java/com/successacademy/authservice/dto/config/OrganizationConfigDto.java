package com.successacademy.authservice.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationConfigDto {

    private String address;
    private String city;
    private String state;
    private String pincode;
    private String country;
    private String contactEmail;
    private String contactPhone;
    private String workingHours;
    private String officeHours;
    private String curriculum;
    private String affiliation;
    private String medium;
    private String mapEmbedUrl;
}
