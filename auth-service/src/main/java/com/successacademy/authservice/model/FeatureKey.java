package com.successacademy.authservice.model;

import java.util.List;

public final class FeatureKey {

    public static final String ATTENDANCE = "ATTENDANCE";
    public static final String FEES = "FEES";
    public static final String EXAMINATIONS = "EXAMINATIONS";
    public static final String ACTIVITIES = "ACTIVITIES";
    public static final String NOTICES = "NOTICES";
    public static final String EVENTS = "EVENTS";
    public static final String GALLERY = "GALLERY";
    public static final String COMMUNICATION = "COMMUNICATION";
    public static final String STAFF = "STAFF";
    public static final String FACULTY = "FACULTY";
    public static final String ACADEMIC_SESSIONS = "ACADEMIC_SESSIONS";

    public static final List<String> ALL_FEATURES = List.of(
            ATTENDANCE,
            FEES,
            EXAMINATIONS,
            ACTIVITIES,
            NOTICES,
            EVENTS,
            GALLERY,
            COMMUNICATION,
            STAFF,
            FACULTY,
            ACADEMIC_SESSIONS
    );

    private FeatureKey() {}
}
