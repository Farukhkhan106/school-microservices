package com.successacademy.academicservice.model;

import lombok.Getter;

@Getter
public enum ActivityType {
    HOMEWORK("Homework"),
    ASSIGNMENT("Assignment"),
    CLASS_TEST("Class Test"),
    WEEKLY_TEST("Weekly Test"),
    MONTHLY_TEST("Monthly Test"),
    UNIT_TEST("Unit Test"),
    SURPRISE_TEST("Surprise Test"),
    RANDOM_TEST("Random Test"),
    PRACTICE_TEST("Practice Test"),
    PROJECT("Project"),
    WORKSHEET("Worksheet"),
    ORAL_TEST("Oral Test"),
    PRACTICAL("Practical Activity"),
    OTHER("Other Activity");

    private final String displayName;

    ActivityType(String displayName) {
        this.displayName = displayName;
    }
}
