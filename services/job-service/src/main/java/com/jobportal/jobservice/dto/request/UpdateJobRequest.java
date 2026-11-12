package com.jobportal.jobservice.dto.request;

import com.jobportal.jobservice.enums.EmploymentType;
import com.jobportal.jobservice.enums.WorkPlaceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateJobRequest(
        @NotBlank
        @Size(max = 200)
        String title,

        @NotBlank
        String description,

        @NotBlank
        @Size(max = 150)
        String location,

        @NotNull
        EmploymentType employmentType,

        @NotNull
        WorkPlaceType workPlaceType,

        @DecimalMin("0.00")
        BigDecimal salaryMin,

        @DecimalMin("0.00")
        BigDecimal salaryMax,

        @Size(min = 3, max = 3)
        String salaryCurrency

) {
}
