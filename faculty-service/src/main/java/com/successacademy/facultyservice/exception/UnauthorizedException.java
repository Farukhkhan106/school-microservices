package com.successacademy.facultyservice.exception;

/** 401 — no/invalid identity headers (request did not come through the gateway). */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}