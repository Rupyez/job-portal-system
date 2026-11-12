
package com.jobportal.jobservice.repository;

import com.jobportal.jobservice.entity.Job;
import com.jobportal.jobservice.enums.JobStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface JobRepository
        extends JpaRepository<Job, UUID>,
        JpaSpecificationExecutor<Job> {

    boolean existsByIdAndEmployerId(
            UUID id,
            UUID employerId
    );

    long countByEmployerId(UUID employerId);

    long countByEmployerIdAndStatus(
            UUID employerId,
            JobStatus status
    );
}
