package com.jobportal.jobpreferencesservice.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles resource not found errors.
     */

    @ExceptionHandler(JobPreferenceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(JobPreferenceNotFoundException exception){
        return buildErrorResponse(HttpStatus.NOT_FOUND, exception.getMessage());
    }


    /**
     * Handles duplicate job preference creation.
     */
    @ExceptionHandler(JobPreferenceAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyExists(JobPreferenceAlreadyExistsException exception){
        return buildErrorResponse(HttpStatus.CONFLICT, exception.getMessage());
    }


    /**
     * Handles business validation errors.
     */
    @ExceptionHandler(InvalidJobPreferenceException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessValidation(InvalidJobPreferenceException exception){
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleRequestValidation(MethodArgumentNotValidException exception){
        Map<String, String> validationErrors = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error -> validationErrors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> response = createBaseErrorResponse(HttpStatus.BAD_REQUEST, "Request validation failed");
        response.put("ValidationErrors", validationErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(response);

    }



    /**
     * Handles invalid path parameter or request parameter types.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid value for parameter: "
                        + exception.getName()
        );
    }


    /**
     * Handles invalid JSON request bodies.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRequestBody(
            HttpMessageNotReadableException exception
    ) {

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid request body"
        );
    }



    /**
     * Handles validation errors outside request DTO binding.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException exception
    ) {

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }



    /**
     * Handles invalid business arguments.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException exception
    ) {

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }




    /**
     * Handles database constraint violations.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException exception
    ) {

        log.warn("Database constraint violation occurred");

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Request conflicts with existing data"
        );
    }



    /**
     * Handles unexpected application exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(
            Exception exception
    ) {

        log.error("Unexpected application error", exception);

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
    }





    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> response = createBaseErrorResponse(status, message);

        return ResponseEntity
                .status(status)
                .body(response);
    }

    private Map<String, Object> createBaseErrorResponse(HttpStatus status, String message) {
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("timeStamp", Instant.now());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", message);
        response.put("path", null);

        return response;
    }


}
