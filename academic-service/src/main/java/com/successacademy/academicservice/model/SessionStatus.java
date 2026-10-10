package com.successacademy.academicservice.model;

public enum SessionStatus {
    DRAFT,
    UPCOMING,
    ACTIVE,
    CLOSING,
    CLOSED,
    ARCHIVED;

    public boolean canTransitionTo(SessionStatus next) {
        if (this == next) return true;
        return switch (this) {
            case DRAFT -> next == UPCOMING || next == ACTIVE || next == ARCHIVED;
            case UPCOMING -> next == ACTIVE || next == DRAFT || next == ARCHIVED;
            case ACTIVE -> next == CLOSING || next == CLOSED;
            case CLOSING -> next == CLOSED || next == ACTIVE;
            case CLOSED -> next == ARCHIVED;
            case ARCHIVED -> false;
        };
    }
}
