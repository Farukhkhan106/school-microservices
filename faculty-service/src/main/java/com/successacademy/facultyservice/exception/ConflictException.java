package com.successacademy.facultyservice.exception;

/** 409 — business rule conflict (duplicate class teacher, timetable clash, duplicate assignment). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}