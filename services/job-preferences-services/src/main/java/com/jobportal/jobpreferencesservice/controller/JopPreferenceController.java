package com.jobportal.jobpreferencesservice.controller;

import com.jobportal.jobpreferencesservice.dto.request.JobPreferenceRequest;
import com.jobportal.jobpreferencesservice.dto.response.JobPreferenceResponse;
import com.jobportal.jobpreferencesservice.service.JobPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/job-preferences")
@RequiredArgsConstructor

public class JopPreferenceController {
    private final JobPreferenceService jobPreferenceService;

    //creates job preferences for a user.


    @PostMapping("/{userId}")
    public ResponseEntity<JobPreferenceResponse> createJobPreference(@PathVariable UUID userId, @Valid @RequestBody JobPreferenceRequest request){
        JobPreferenceResponse response = jobPreferenceService.createJobPreference(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);

    }

    @GetMapping("/{userId}")
    public ResponseEntity<JobPreferenceResponse> getJobPreference(@PathVariable UUID userId){
        JobPreferenceResponse response = jobPreferenceService.getJobPreferenceByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<JobPreferenceResponse> updateJobPreference(@PathVariable UUID userId, @Valid @RequestBody JobPreferenceRequest request){
        JobPreferenceResponse response = jobPreferenceService.updateJobPreference(userId, request);

        return ResponseEntity.ok(response);

    }


    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteJobPreference(@PathVariable UUID userId){
        jobPreferenceService.deleteJobPreference(userId);
        return ResponseEntity.noContent().build();
    }
}
