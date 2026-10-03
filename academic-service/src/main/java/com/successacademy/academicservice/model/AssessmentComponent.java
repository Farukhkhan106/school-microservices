package com.successacademy.academicservice.model;

public enum AssessmentComponent {
    THEORY("Theory Exam"),
    PRACTICAL("Practical / Lab"),
    VIVA("Oral / Viva Voce"),
    NOTEBOOK("Notebook & Regularity"),
    PROJECT("Project & Exhibition"),
    PORTFOLIO("Subject Portfolio");

    private final String displayName;

    AssessmentComponent(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
