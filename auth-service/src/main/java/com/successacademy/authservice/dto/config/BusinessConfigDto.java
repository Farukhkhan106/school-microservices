package com.successacademy.authservice.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessConfigDto {
    private String currency;
    private String timezone;
    private String dateFormat;
    private String academicYearStartMonth; // e.g. "APRIL"
    private Map<String, Object> customRules;
}
