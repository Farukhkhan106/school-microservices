package com.successacademy.facultyservice.exception;

/** 403 — authenticated but not allowed (e.g. subject teacher updating attendance). */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}