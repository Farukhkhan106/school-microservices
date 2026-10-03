package com.successacademy.academicservice.repository;

import com.successacademy.academicservice.model.GradingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GradingRuleRepository extends JpaRepository<GradingRule, Long> {
    List<GradingRule> findBySchemeIdOrderByMinPercentageDesc(Long schemeId);
}
