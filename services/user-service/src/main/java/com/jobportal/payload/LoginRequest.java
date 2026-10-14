package com.jobportal.payload;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class LoginRequest {

    @Email(message = "Email should be valid")
    @NotBlank(message = "email is required")
    private String email;


    @NotBlank(message = "password is required")
    private String password;
}
