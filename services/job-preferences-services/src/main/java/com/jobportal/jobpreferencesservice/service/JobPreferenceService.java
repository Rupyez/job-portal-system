package com.jobportal.jobpreferencesservice.service;

import com.jobportal.jobpreferencesservice.dto.request.JobPreferenceRequest;
import com.jobportal.jobpreferencesservice.dto.response.JobPreferenceResponse;



public interface JobPreferenceService {

    /**
     * Creates job preferences for a user.
     *
     * @param userId user ID
     * @param request job preference details
     * @return created job preferences
     */
    JobPreferenceResponse createJobPreference(
            Long userId,
            JobPreferenceRequest request
    );

    /**
     * Retrieves job preferences belonging to a user.
     *
     * @param userId user ID
     * @return user's job preferences
     */
    JobPreferenceResponse getJobPreferenceByUserId(
            Long userId
    );

    /**
     * Updates existing job preferences.
     *
     * @param userId user ID
     * @param request updated preference details
     * @return updated job preferences
     */
    JobPreferenceResponse updateJobPreference(
            Long userId,
            JobPreferenceRequest request
    );

    /**
     * Deletes job preferences belonging to a user.
     *
     * @param userId user ID
     */
    void deleteJobPreference(
            Long userId
    );
}