package com.jobportal.companyservice.dto;


import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class CompanyResponse {

    private Long id;

    private String name;

    private String description;

    private String website;

    private String industry;

    private String companySize;

    private String location;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
