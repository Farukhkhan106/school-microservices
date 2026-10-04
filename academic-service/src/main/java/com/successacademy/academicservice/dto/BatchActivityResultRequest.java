package com.successacademy.academicservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchActivityResultRequest {

    @NotEmpty(message = "Results list cannot be empty")
    @Valid
    private List<ActivityResultEntryRequest> entries;
}
