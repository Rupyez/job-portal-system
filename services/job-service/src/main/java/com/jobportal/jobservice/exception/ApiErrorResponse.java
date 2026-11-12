package com.jobportal.jobservice.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<String> details
) {

    public static ApiErrorResponse of(
            HttpStatus status,
            String message
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                List.of()
        );
    }


    public static ApiErrorResponse of(
            HttpStatus status,
            String message,
            List<String> details
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                details
        );
    }

}
