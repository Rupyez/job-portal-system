package com.jobportal.jobpreferencesservice.repository;

import com.jobportal.jobpreferencesservice.entity.JobPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface JobPreferenceRepository extends JpaRepository<JobPreference, Long> {

    /**
     * Finds job preferences belonging to a specific user.
     * Each user can have only one job preference record.
     *
     * @param userId authenticated user's ID
     * @return user's job preferences if they exist
     */
    Optional<JobPreference> findByUserId(Long userId);

    /**
     * Checks whether a user already has job preferences.
     *
     * @param userId authenticated user's ID
     * @return true if preferences exist
     */
    boolean existsByUserId(Long userId);

    /**
     * Deletes job preferences belonging to a specific user.
     *
     * @param userId authenticated user's ID
     */
    void deleteByUserId(Long userId);
}
