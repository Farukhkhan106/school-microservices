package com.successacademy.academicservice.service;

import com.successacademy.academicservice.model.GradingRule;
import com.successacademy.academicservice.model.GradingScheme;

import java.math.BigDecimal;

public interface GradingService {
    GradingScheme getDefaultScheme();
    GradingRule calculateGrade(BigDecimal percentage, GradingScheme scheme);
    GradingRule calculateGrade(BigDecimal marksObtained, BigDecimal maxMarks, GradingScheme scheme);
}
