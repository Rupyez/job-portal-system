package com.jobportal.jobpreferencesservice.exception;


public class JobPreferenceNotFoundException extends RuntimeException {

    public JobPreferenceNotFoundException(Long userId) {
        super("Job preferences not found for user: " + userId);
    }
}