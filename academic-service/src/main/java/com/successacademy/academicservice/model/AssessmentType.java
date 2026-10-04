package com.successacademy.academicservice.model;

public enum AssessmentType {
    PERIOD_TEST("Period / In-Class Test"),
    UNIT_TEST("Periodic Assessment (UT)"),
    WEEKLY_TEST("Weekly Test"),
    MONTHLY_TEST("Monthly Assessment"),
    QUARTERLY("Quarterly Examination"),
    HALF_YEARLY("Half-Yearly / Mid-Term"),
    PRE_BOARD("Pre-Board / Model Exam"),
    ANNUAL("Annual / Final Examination"),
    INTERNAL_ASSESSMENT("Internal Assessment");

    private final String displayName;

    AssessmentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAllowedForNewAssessment() {
        return switch (this) {
            case UNIT_TEST, QUARTERLY, HALF_YEARLY, PRE_BOARD, ANNUAL -> true;
            default -> false;
        };
    }
}
