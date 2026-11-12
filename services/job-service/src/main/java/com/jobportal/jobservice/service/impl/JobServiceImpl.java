
package com.jobportal.jobservice.service.impl;

import com.jobportal.jobservice.dto.request.CreateJobRequest;
import com.jobportal.jobservice.dto.request.UpdateJobRequest;
import com.jobportal.jobservice.dto.response.JobResponse;
import com.jobportal.jobservice.entity.Job;
import com.jobportal.jobservice.enums.JobStatus;
import com.jobportal.jobservice.exception.JobNotFoundException;
import com.jobportal.jobservice.repository.JobRepository;


import com.jobportal.jobservice.service.service.JobService;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;

    public JobServiceImpl(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Override
    @Transactional
    public JobResponse createJob(CreateJobRequest request) {

        Job job = Job.create(
                request.title(),
                request.description(),
                request.employerId(),
                request.location(),
                request.employmentType(),
                request.workPlaceType()
        );

        job.updateCompensation(
                request.salaryMin(),
                request.salaryMax(),
                request.salaryCurrency()
        );

        Job savedJob = jobRepository.save(job);

        return JobResponse.from(savedJob);
    }

    @Override
    public JobResponse getJobById(UUID jobId) {

        Job job = findJobById(jobId);

        return JobResponse.from(job);
    }

    @Override
    public Page<JobResponse> searchJobs(
            String keyword,
            String location,
            JobStatus status,
            Pageable pageable
    ) {

        Specification<Job> specification = buildSearchSpecification(
                keyword,
                location,
                status
        );

        return jobRepository.findAll(specification, pageable)
                .map(JobResponse::from);
    }

    @Override
    public Page<JobResponse> getEmployerJobs(
            UUID employerId,
            Pageable pageable
    ) {

        Specification<Job> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(
                                root.get("employerId"),
                                employerId
                        );

        return jobRepository.findAll(specification, pageable)
                .map(JobResponse::from);
    }

    @Override
    @Transactional
    public JobResponse updateJob(
            UUID jobId,
            UpdateJobRequest request
    ) {

        Job job = findJobById(jobId);

        job.updateDetails(
                request.title(),
                request.description(),
                request.location()
        );

        job.updateClassification(
                request.employmentType(),
                request.workPlaceType()
        );

        job.updateCompensation(
                request.salaryMin(),
                request.salaryMax(),
                request.salaryCurrency()
        );

        return JobResponse.from(job);
    }

    @Override
    @Transactional
    public JobResponse publishJob(UUID jobId) {

        Job job = findJobById(jobId);

        job.publish();

        return JobResponse.from(job);
    }

    @Override
    @Transactional
    public JobResponse closeJob(UUID jobId) {

        Job job = findJobById(jobId);

        job.close();

        return JobResponse.from(job);
    }

    @Override
    @Transactional
    public void deleteJob(UUID jobId) {

        Job job = findJobById(jobId);

        jobRepository.delete(job);
    }

    private Job findJobById(UUID jobId) {

        return jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new JobNotFoundException(jobId)
                );
    }

    private Specification<Job> buildSearchSpecification(
            String keyword,
            String location,
            JobStatus status
    ) {

        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {

                String searchKeyword = "%"
                        + keyword.trim().toLowerCase(Locale.ROOT)
                        + "%";

                Predicate titlePredicate =
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("title")
                                ),
                                searchKeyword
                        );

                Predicate descriptionPredicate =
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("description")
                                ),
                                searchKeyword
                        );

                predicates.add(
                        criteriaBuilder.or(
                                titlePredicate,
                                descriptionPredicate
                        )
                );
            }

            if (location != null && !location.isBlank()) {

                String searchLocation = "%"
                        + location.trim().toLowerCase(Locale.ROOT)
                        + "%";

                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("location")
                                ),
                                searchLocation
                        )
                );
            }

            if (status != null) {

                predicates.add(
                        criteriaBuilder.equal(
                                root.get("status"),
                                status
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }
}
