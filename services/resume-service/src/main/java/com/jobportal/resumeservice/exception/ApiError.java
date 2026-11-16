package com.jobportal.resumeservice.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.validation.FieldError;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String code,
        String message,
        Instant timestamp,
        String path,
        List<FieldViolation> fieldErrors
) {

    public record FieldViolation(String field, String message) {}

    public static ApiError of(String code, String message, String path) {
        return new ApiError(code, message, Instant.now(), path, null);
    }

    public static ApiError of(String code, String message, String path,
                              List<FieldViolation> fieldErrors) {
        return new ApiError(code, message, Instant.now(), path, fieldErrors);
    }
}