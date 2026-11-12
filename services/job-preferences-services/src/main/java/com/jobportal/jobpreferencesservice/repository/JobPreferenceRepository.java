package com.jobportal.jobpreferencesservice.repository;

import com.jobportal.jobpreferencesservice.entity.JobPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobPreferenceRepository extends JpaRepository<JobPreference, UUID> {

    /**
     * Finds job preferences belonging to a specific user.
     *
     * Each user can have only one job preference record.
     *
     * @param userId authenticated user's ID
     * @return user's job preferences if they exist
     */
    Optional<JobPreference> findByUserId(UUID userId);

    /**
     * Checks whether a user already has job preferences.
     *
     * @param userId authenticated user's ID
     * @return true if preferences exist
     */
    boolean existsByUserId(UUID userId);

    /**
     * Deletes job preferences belonging to a specific user.
     *
     * @param userId authenticated user's ID
     */
    void deleteByUserId(UUID userId);
}
