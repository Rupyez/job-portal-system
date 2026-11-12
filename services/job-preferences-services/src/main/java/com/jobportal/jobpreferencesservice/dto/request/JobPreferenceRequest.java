package com.jobportal.jobpreferencesservice.dto.request;

import com.jobportal.jobpreferencesservice.enums.EmploymentType;
import com.jobportal.jobpreferencesservice.enums.WorkMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPreferenceRequest {

    @Size(
            max = 20,
            message = "Job titles cannot contain more than 20 entries"
    )
    private List<
                @NotNull(message = "Job title cannot be null")
                @Size(
                        min = 2,
                        max = 100,
                        message = "Job title must be between 2 and 100 characters"
                )
                        String
                > jobTitles;

    @Size(
            max = 20,
            message = "Locations cannot contain more than 20 entries"
    )
    private List<
            @NotNull(message = "Location cannot be null")
            @Size(
                    min = 2,
                    max = 100,
                    message = "Location must be between 2 and 100 characters"
            )
                    String
            > locations;

    @Size(
            max = 30,
            message = "Skills cannot contain more than 30 entries"
    )
    private List<
            @NotNull(message = "Skill cannot be null")
            @Size(
                    min = 2,
                    max = 50,
                    message = "Skill must be between 2 and 50 characters"
            )
                    String
            > skills;

    @Size(
            max = 20,
            message = "Industries cannot contain more than 20 entries"
    )
    private List<
            @NotNull(message = "Industry cannot be null")
            @Size(
                    min = 2,
                    max = 100,
                    message = "Industry must be between 2 and 100 characters"
            )
                    String
            > industries;

    private EmploymentType employmentType;

    private WorkMode workMode;

    @DecimalMin(
            value = "0.00",
            message = "Minimum salary cannot be negative"
    )
    private BigDecimal minimumSalary;

    @DecimalMin(
            value = "0.00",
            message = "Maximum salary cannot be negative"
    )
    private BigDecimal maximumSalary;

    @Pattern(
            regexp = "^[A-Z]{3}$",
            message = "Currency must be a valid three-letter uppercase code"
    )
    private String currency;
}
