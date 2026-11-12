package com.jobportal.companyservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class CompanyRequest {

    @NotBlank(message = "Company name is required")
    @Size(max = 150, message = "Company name cannot exceed 150 characters")
    private String name;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @Size(max = 255, message = "Website cannot exceed 255 characters")
    private String website;

    @Size(max = 100, message = "Industry cannot exceed 100 characters")
    private String industry;

    @Size(max = 50, message = "Company size cannot exceed 50 characters")
    private String companySize;

    @Size(max = 150, message = "Location cannot exceed 150 characters")
    private String location;
}
