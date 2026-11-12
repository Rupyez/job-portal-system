package com.jobportal.jobservice.controller;


import com.jobportal.jobservice.dto.request.CreateJobRequest;
import com.jobportal.jobservice.dto.request.UpdateJobRequest;
import com.jobportal.jobservice.dto.response.JobResponse;
import com.jobportal.jobservice.enums.JobStatus;
import com.jobportal.jobservice.service.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.UUID;


/**
 * REST API for job postings.
 *
 * <p>Design notes:
 * <ul>
 *   <li>Collection lives at {@code /jobs}; a single job at {@code /jobs/{id}}.</li>
 *   <li>Employer-scoped listing is a nested resource:
 *       {@code /employers/{employerId}/jobs} — the query is scoped to an employer,
 *       so the URL reflects that.</li>
 *   <li>State transitions are action sub-resources ({@code POST /jobs/{id}/publish},
 *       {@code /close}) so they map 1:1 onto the domain methods on {@code Job};
 *       callers cannot bypass the state machine via a generic PATCH.</li>
 *   <li>{@code POST /jobs} returns {@code 201 Created} with a {@code Location} header.
 *       {@code DELETE} returns {@code 204 No Content}.</li>
 *   <li>Pagination is Spring-native: {@code ?page=&size=&sort=} via {@link Pageable}.
 *       Sensible defaults are declared with {@link PageableDefault}.</li>
 * </ul>
 */

@Validated
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor


public class JobController {

    private final JobService jobService;


    @PostMapping
    public ResponseEntity<JobResponse> create(@Valid @RequestBody CreateJobRequest request){
        JobResponse created = jobService.createJob(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }


    @GetMapping("/jobs/{jobId}")
    public JobResponse getById(@PathVariable UUID jobId) {
        return jobService.getJobById(jobId);
    }



    @GetMapping
    public ResponseEntity<Page<JobResponse>> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) JobStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(
                jobService.searchJobs(
                        keyword,
                        location,
                        status,
                        pageable
                )
        );
    }


    @GetMapping("/employers/{employerId}/jobs")
    public Page<JobResponse> getEmployerJobs(
            @PathVariable UUID employerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)Pageable pageable
            ){
        return jobService.getEmployerJobs(employerId,pageable);
    }



    @PutMapping("/jobs/{jobId}")
    public JobResponse update(@PathVariable UUID jobId, @Valid @RequestBody UpdateJobRequest request){
        return jobService.updateJob(jobId, request);
    }


    @PostMapping("/jobs/{jobId}/publish")
    public JobResponse publish(@PathVariable UUID jobId){
        return jobService.publishJob(jobId);
    }


    @PostMapping("/jobs/{jobId}/close")
    public JobResponse close(@PathVariable UUID jobId){
        return jobService.closeJob(jobId);
    }


    @PostMapping("/jobs/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID jobId){
        jobService.deleteJob(jobId);
    }

}
