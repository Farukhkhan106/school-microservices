package com.successacademy.authservice.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigDiffResponseDto {
    private Long baseVersionId;
    private Integer baseVersionNumber;
    private Long targetVersionId;
    private Integer targetVersionNumber;
    @Builder.Default
    private int addedCount = 0;
    @Builder.Default
    private int removedCount = 0;
    @Builder.Default
    private int changedCount = 0;
    @Builder.Default
    private int unchangedCount = 0;
    @Builder.Default
    private List<ConfigDiffItemDto> diffs = new ArrayList<>();
}
