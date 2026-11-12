package com.jobportal.jobpreferencesservice.dto.response;

import com.jobportal.jobpreferencesservice.enums.EmploymentType;
import com.jobportal.jobpreferencesservice.enums.WorkMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPreferenceResponse {

    private UUID id;

    private UUID userId;

    private List<String> jobTitles;

    private List<String> locations;

    private List<String> skills;

    private List<String> industries;

    private EmploymentType employmentType;

    private WorkMode workMode;

    private BigDecimal minimumSalary;

    private BigDecimal maximumSalary;

    private String currency;

    private Instant createdAt;

    private Instant updatedAt;

    private Long version;
}
