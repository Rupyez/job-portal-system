package com.jobportal.jobpreferencesservice.exception;



public class JobPreferenceAlreadyExistsException extends RuntimeException {

    public JobPreferenceAlreadyExistsException(Long userId) {
        super("Job preferences already exist for user: " + userId);
    }
}