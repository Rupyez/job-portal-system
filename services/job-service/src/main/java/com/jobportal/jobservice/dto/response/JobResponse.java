package com.jobportal.jobservice.dto.response;

import com.jobportal.jobservice.entity.Job;
import com.jobportal.jobservice.enums.EmploymentType;
import com.jobportal.jobservice.enums.JobStatus;
import com.jobportal.jobservice.enums.WorkPlaceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record JobResponse(
        UUID id,
        String title,
        String description,
        UUID employerId,
        String location,
        EmploymentType employmentType,
        WorkPlaceType workPlaceType,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String salaryCurrency,
        JobStatus status,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {

    public static JobResponse from(Job job){
        return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getEmployerId(),
                job.getLocation(),
                job.getEmploymentType(),
                job.getWorkPlaceType(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getSalaryCurrency(),
                job.getStatus(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                job.getVersion()
        );
    }
}
