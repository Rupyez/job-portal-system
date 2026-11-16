package com.jobportal.resumeservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResumeUpdateRequest (
        @NotBlank(message = "Resume title is required.")
        @Size(max = 150, message = "Title must be at most 150 characters.")
        String title,

        @Size(max = 2000, message = "Summary must be at most 2000 characters.")
        String summary


){
}
