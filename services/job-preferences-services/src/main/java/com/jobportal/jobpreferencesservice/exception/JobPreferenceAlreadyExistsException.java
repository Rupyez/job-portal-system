package com.jobportal.jobpreferencesservice.exception;

import java.util.UUID;

public class JobPreferenceAlreadyExistsException extends RuntimeException {

    public JobPreferenceAlreadyExistsException(UUID userId) {
        super("Job preferences already exist for user: " + userId);
    }
}