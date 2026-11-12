package com.jobportal.jobservice.exception;

import java.util.UUID;

public class JobNotFoundException extends RuntimeException {
    public JobNotFoundException(UUID jobId) {
        super("Job not found with ID: " + jobId);
    }

}
