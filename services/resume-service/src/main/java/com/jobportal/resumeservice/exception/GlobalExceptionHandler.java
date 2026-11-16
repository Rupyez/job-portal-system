package com.jobportal.resumeservice.exception;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;


import javax.naming.AuthenticationException;
import java.nio.file.AccessDeniedException;
import java.util.List;


@Slf4j
@RestControllerAdvice

public class GlobalExceptionHandler {

    // ------------------------------------------------------------------
    // 404 — resume not found / not owned
    // ------------------------------------------------------------------
    @ExceptionHandler(ResumeNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResumeNotFoundException ex,
                                                   HttpServletRequest req) {
        log.debug("404 {}: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("RESUME_NOT_FOUND", ex.getMessage(), req.getRequestURI()));
    }

    // ------------------------------------------------------------------
    // 400 — bean validation on @RequestBody / @ModelAttribute
    // ------------------------------------------------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest req) {
        List<ApiError.FieldViolation> fields = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldViolation)
                .toList();

        return ResponseEntity.badRequest().body(ApiError.of(
                "VALIDATION_FAILED",
                "Request validation failed.",
                req.getRequestURI(),
                fields
        ));
    }

    // ------------------------------------------------------------------
    // 400 — missing multipart part (e.g. client forgot "file")
    // ------------------------------------------------------------------
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiError> handleMissingPart(MissingServletRequestPartException ex,
                                                      HttpServletRequest req) {
        return ResponseEntity.badRequest().body(ApiError.of(
                "MISSING_PART",
                "Required part missing: " + ex.getRequestPartName(),
                req.getRequestURI()
        ));
    }

    // ------------------------------------------------------------------
    // 400 — domain validation from entity / service
    // ------------------------------------------------------------------
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex,
                                                          HttpServletRequest req) {
        return ResponseEntity.badRequest().body(ApiError.of(
                "BAD_REQUEST", ex.getMessage(), req.getRequestURI()
        ));
    }

    // ------------------------------------------------------------------
    // 413 — upload larger than configured limit
    // ------------------------------------------------------------------
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleTooLarge(MaxUploadSizeExceededException ex,
                                                   HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(ApiError.of(
                "FILE_TOO_LARGE",
                "Uploaded file exceeds the maximum allowed size.",
                req.getRequestURI()
        ));
    }

    // ------------------------------------------------------------------
    // 401 — no authentication (belt-and-braces; Spring usually handles)
    // ------------------------------------------------------------------
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleUnauthenticated(AuthenticationException ex,
                                                          HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiError.of(
                "UNAUTHENTICATED", "Authentication required.", req.getRequestURI()
        ));
    }

    // ------------------------------------------------------------------
    // 403 — authenticated but not allowed
    // ------------------------------------------------------------------
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleForbidden(AccessDeniedException ex,
                                                    HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.of(
                "FORBIDDEN", "Access denied.", req.getRequestURI()
        ));
    }

    // ------------------------------------------------------------------
    // 500 — anything else (log full stack trace, don't leak details)
    // ------------------------------------------------------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex,
                                                     HttpServletRequest req) {
        log.error("Unhandled exception at {}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.of(
                "INTERNAL_ERROR",
                "An unexpected error occurred.",
                req.getRequestURI()
        ));
    }

    // ------------------------------------------------------------------
    private ApiError.FieldViolation toFieldViolation(FieldError fe) {
        return new ApiError.FieldViolation(fe.getField(), fe.getDefaultMessage());
    }
}