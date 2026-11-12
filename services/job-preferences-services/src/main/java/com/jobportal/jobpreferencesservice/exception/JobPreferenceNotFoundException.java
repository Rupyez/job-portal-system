package com.jobportal.jobpreferencesservice.exception;

import java.util.UUID;

public class JobPreferenceNotFoundException extends RuntimeException {

    public JobPreferenceNotFoundException(UUID userId) {
        super("Job preferences not found for user: " + userId);
    }
}