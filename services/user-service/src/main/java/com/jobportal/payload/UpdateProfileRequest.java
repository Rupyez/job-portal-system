
package com.jobportal.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phoneNumber;

    @Size(max = 500, message = "Profile image URL cannot exceed 500 characters")
    private String profileImage;
}
