package com.successacademy.academicservice.controller;

import com.successacademy.academicservice.dto.ClassPerformanceAnalyticsResponse;
import com.successacademy.academicservice.dto.GradingRuleDto;
import com.successacademy.academicservice.dto.GradingSchemeResponse;
import com.successacademy.academicservice.model.GradingScheme;
import com.successacademy.academicservice.security.SecurityContextUtil;
import com.successacademy.academicservice.service.AnalyticsService;
import com.successacademy.academicservice.service.GradingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/academic/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final GradingService gradingService;
    private final SecurityContextUtil securityUtil;

    @GetMapping("/class")
    public ResponseEntity<ClassPerformanceAnalyticsResponse> getClassAnalytics(
            @RequestParam Long assessmentId,
            @RequestParam String studentClass,
            @RequestParam String section,
            HttpServletRequest req
    ) {
        securityUtil.requireAdminOrTeacher(req);
        return ResponseEntity.ok(analyticsService.getClassAnalytics(assessmentId, studentClass, section));
    }

    @GetMapping("/grading-scheme/default")
    public ResponseEntity<GradingSchemeResponse> getDefaultGradingScheme() {
        GradingScheme scheme = gradingService.getDefaultScheme();
        if (scheme == null) return ResponseEntity.notFound().build();

        var ruleDtos = scheme.getRules().stream().map(r -> GradingRuleDto.builder()
                .id(r.getId())
                .grade(r.getGrade())
                .minPercentage(r.getMinPercentage())
                .maxPercentage(r.getMaxPercentage())
                .gradePoint(r.getGradePoint())
                .description(r.getDescription())
                .isPassing(r.isPassing())
                .build()).toList();

        return ResponseEntity.ok(GradingSchemeResponse.builder()
                .id(scheme.getId())
                .name(scheme.getName())
                .description(scheme.getDescription())
                .isDefault(scheme.isDefault())
                .rules(ruleDtos)
                .build());
    }
}
