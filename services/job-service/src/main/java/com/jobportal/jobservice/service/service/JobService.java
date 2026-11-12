package com.jobportal.jobservice.service.service;

import com.jobportal.jobservice.dto.request.CreateJobRequest;
import com.jobportal.jobservice.dto.request.UpdateJobRequest;
import com.jobportal.jobservice.dto.response.JobResponse;
import com.jobportal.jobservice.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.UUID;

public interface JobService {

    JobResponse createJob(CreateJobRequest request);

    JobResponse getJobById(UUID jobId);

    Page<JobResponse> searchJobs(
            String keyword,
            String location,
            JobStatus status,
            Pageable pageable
    );

    Page<JobResponse> getEmployerJobs(
            UUID employerId,
            Pageable pageable
    );

    JobResponse updateJob(
            UUID jobId,
            UpdateJobRequest request
    );

    JobResponse publishJob(UUID jobId);

    JobResponse closeJob(UUID jobId);

    void deleteJob(UUID jobId);
}
