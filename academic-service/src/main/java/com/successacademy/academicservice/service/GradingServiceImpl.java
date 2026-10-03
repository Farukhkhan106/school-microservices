package com.successacademy.academicservice.service;

import com.successacademy.academicservice.model.GradingRule;
import com.successacademy.academicservice.model.GradingScheme;
import com.successacademy.academicservice.repository.GradingRuleRepository;
import com.successacademy.academicservice.repository.GradingSchemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GradingServiceImpl implements GradingService {

    private final GradingSchemeRepository schemeRepository;
    private final GradingRuleRepository ruleRepository;

    @Override
    public GradingScheme getDefaultScheme() {
        return schemeRepository.findByIsDefaultTrue()
                .orElseGet(() -> schemeRepository.findAll().stream().findFirst().orElse(null));
    }

    @Override
    public GradingRule calculateGrade(BigDecimal percentage, GradingScheme scheme) {
        if (percentage == null) return null;

        GradingScheme targetScheme = scheme != null ? scheme : getDefaultScheme();
        if (targetScheme == null) {
            // Fallback hardcoded CBSE standard if database rules not yet seeded
            return createFallbackRule(percentage);
        }

        List<GradingRule> rules = targetScheme.getRules();
        if (rules == null || rules.isEmpty()) {
            rules = ruleRepository.findBySchemeIdOrderByMinPercentageDesc(targetScheme.getId());
        }

        for (GradingRule r : rules) {
            if (percentage.compareTo(r.getMinPercentage()) >= 0 && percentage.compareTo(r.getMaxPercentage()) <= 0) {
                return r;
            }
        }

        // If above max or below min
        if (!rules.isEmpty()) {
            if (percentage.compareTo(rules.get(0).getMaxPercentage()) > 0) {
                return rules.get(0); // Top grade
            }
            return rules.get(rules.size() - 1); // Lowest grade
        }

        return createFallbackRule(percentage);
    }

    @Override
    public GradingRule calculateGrade(BigDecimal marksObtained, BigDecimal maxMarks, GradingScheme scheme) {
        if (marksObtained == null || maxMarks == null || maxMarks.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal pct = marksObtained.multiply(BigDecimal.valueOf(100))
                .divide(maxMarks, 2, RoundingMode.HALF_UP);
        return calculateGrade(pct, scheme);
    }

    private GradingRule createFallbackRule(BigDecimal pct) {
        double p = pct.doubleValue();
        if (p >= 91.0) return buildRule("A1", 10.0, true, "Outstanding");
        if (p >= 81.0) return buildRule("A2", 9.0, true, "Excellent");
        if (p >= 71.0) return buildRule("B1", 8.0, true, "Very Good");
        if (p >= 61.0) return buildRule("B2", 7.0, true, "Good");
        if (p >= 51.0) return buildRule("C1", 6.0, true, "Above Average");
        if (p >= 41.0) return buildRule("C2", 5.0, true, "Average");
        if (p >= 33.0) return buildRule("D", 4.0, true, "Pass");
        return buildRule("E", 0.0, false, "Essential Repeat");
    }

    private GradingRule buildRule(String grade, double gp, boolean isPass, String desc) {
        return GradingRule.builder()
                .grade(grade)
                .gradePoint(BigDecimal.valueOf(gp))
                .isPassing(isPass)
                .description(desc)
                .build();
    }
}
