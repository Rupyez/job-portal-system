package com.jobportal.resumeservice.dto.response;

import com.jobportal.resumeservice.entity.Resume;

import java.time.Instant;
import java.util.UUID;


/**
 * Read model for a resume. Never exposes {@code storedFileName}
 * (internal server-side path) to clients.
 */

public record ResumeResponse(
        UUID id,
        Long userId,
        String title,
        String summary,
        String originalFileName,
        String contentType,
        Long fileSize,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {



    public static ResumeResponse from(Resume resume){
        return new ResumeResponse(
                resume.getId(),
                resume.getUserId(),
                resume.getTitle(),
                resume.getSummary(),
                resume.getOriginalFileName(),
                resume.getContentType(),
                resume.getFileSize(),
                resume.getCreatedAt(),
                resume.getUpdatedAt(),
                resume.getVersion()
        );
    }
}
