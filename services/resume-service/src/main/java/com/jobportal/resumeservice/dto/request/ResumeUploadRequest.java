package com.jobportal.resumeservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record ResumeUploadRequest(

    @NotBlank(message = "Resume title is required.")
    @Size(max = 150)
    String title,

    @Size(max = 2000)
    String summary,

    @NotNull(message = "Resume file is required.")
    MultipartFile file){

}
