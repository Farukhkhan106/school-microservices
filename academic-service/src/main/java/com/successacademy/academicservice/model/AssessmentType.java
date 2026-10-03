package com.successacademy.academicservice.model;

public enum AssessmentType {
    PERIOD_TEST("Period / In-Class Test"),
    UNIT_TEST("Unit Test"),
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
}
