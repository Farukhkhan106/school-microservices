package com.successacademy.authservice.dto.config;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigValidationResultDto {
    private boolean valid;
    @Builder.Default
    private List<ConfigValidationErrorDto> errors = new ArrayList<>();
    @Builder.Default
    private List<ConfigValidationErrorDto> warnings = new ArrayList<>();
}
