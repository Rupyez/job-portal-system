package com.jobportal.jobpreferencesservice.service.impl;

import com.jobportal.jobpreferencesservice.dto.request.JobPreferenceRequest;
import com.jobportal.jobpreferencesservice.dto.response.JobPreferenceResponse;
import com.jobportal.jobpreferencesservice.entity.JobPreference;
import com.jobportal.jobpreferencesservice.repository.JobPreferenceRepository;
import com.jobportal.jobpreferencesservice.service.JobPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;


@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobPreferenceServiceImpl implements JobPreferenceService {

    private final JobPreferenceRepository jobPreferenceRepository;
    @Override
    public JobPreferenceResponse createJobPreference(UUID userId, JobPreferenceRequest request) {
        validateUserId(userId);
        validateRequest(request);
        validateSalaryRange(request.getMinimumSalary(), request.getMaximumSalary());
        
        
        if(jobPreferenceRepository.existsByUserId(userId)){
            throw new IllegalArgumentException("Job Preferences already exists for this user.");
        }
        
        JobPreference jobPreference = JobPreference.builder()
                .userId(userId)
                .preferredJobTitles(request.getJobTitles())
                .preferredLocations(request.getLocations())
                .preferredSkills(request.getSkills())
                .preferredIndustries(request.getIndustries())
                .employmentType(request.getEmploymentType())
                .workMode(request.getWorkMode())
                .minimumSalary(request.getMinimumSalary())
                .maximumSalary(request.getMaximumSalary())
                .currency(request.getCurrency())
                .build();
        
        JobPreference savedPreference = jobPreferenceRepository.save(jobPreference);
        log.info("Created job preferences for user");
        return mapToResponse(savedPreference);
    }

    private JobPreferenceResponse mapToResponse(JobPreference jobPreference) {
        return JobPreferenceResponse.builder()
                .id(jobPreference.getId())
                .userId(jobPreference.getUserId())
                .jobTitles(jobPreference.getPreferredLocations())
                .locations(jobPreference.getPreferredLocations())
                .skills(jobPreference.getPreferredSkills())
                .industries(jobPreference.getPreferredIndustries())
                .employmentType(jobPreference.getEmploymentType())
                .workMode(jobPreference.getWorkMode())
                .minimumSalary(jobPreference.getMinimumSalary())
                .maximumSalary(jobPreference.getMaximumSalary())
                .currency(jobPreference.getCurrency())
                .createdAt(jobPreference.getCreatedAt())
                .updatedAt(jobPreference.getUpdatedAt())
                .build();
    }

    @Override
    public JobPreferenceResponse getJobPreferenceByUserId(UUID userId) {
        validateUserId(userId);
        JobPreference jobPreference = jobPreferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Job Preferences not found."));
        return mapToResponse(jobPreference);
    }



    @Override
    public JobPreferenceResponse updateJobPreference(UUID userId, JobPreferenceRequest request) {
        validateUserId(userId);
        validateRequest(request);

        validateSalaryRange(request.getMinimumSalary(), request.getMaximumSalary());
        JobPreference jobPreference = jobPreferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Job Preferences not found"));


        /*
         * Update the existing entity.
         *
         * Keep the entity's update method responsible for
         * sanitizing collections and applying changes.
         */

        jobPreference.update(
                request.getJobTitles(),
                request.getLocations(),
                request.getSkills(),
                request.getIndustries(),
                request.getEmploymentType(),
                request.getWorkMode(),
                request.getMinimumSalary(),
                request.getMaximumSalary(),
                request.getCurrency()
        );

        JobPreference updatedPreferences = jobPreferenceRepository.save(jobPreference);
        log.info("Updated job preferences for user.");
        return mapToResponse(updatedPreferences);
    }

    @Override
    public void deleteJobPreference(UUID userId) {
        validateUserId(userId);

        JobPreference jobPreference = jobPreferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Job Preference not found"));
        log.info("Deleted job preferences for user");
    }


    /**
     * Validates user ID.
     */

    private void validateUserId(UUID userId){
        if(userId == null){
            throw new IllegalArgumentException("User ID cannot be null");
        }
    }


    /**
     * Validates request object.
     */
    private void validateRequest(JobPreferenceRequest request){
        if(request == null){
            throw new IllegalArgumentException("Job Preference request cannot be null.");
        }
    }

    private void validateSalaryRange(BigDecimal minimumSalary, BigDecimal maximumSalary){
        if (minimumSalary != null && minimumSalary.signum() < 0){
            throw new IllegalArgumentException("Minimum salary cannot be negative.");
        }

        if (maximumSalary != null && maximumSalary.signum() < 0){
            throw new IllegalArgumentException("Maximum salary cannot be negative.");
        }

        if(minimumSalary != null && maximumSalary != null && minimumSalary.compareTo(maximumSalary) > 0){
            throw new IllegalArgumentException("Minimum salary cannot exceed maximum salary");
        }
    }
}
