package com.successacademy.authservice.dto.config;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigValidationErrorDto {
    private String code;
    private String path;
    private String message;
    private String severity; // ERROR, WARNING
}
