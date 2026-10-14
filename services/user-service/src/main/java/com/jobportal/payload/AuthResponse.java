package com.jobportal.payload;

import dto.response.UserResponse;
import lombok.Data;

@Data
public class AuthResponse {

    private String jwt;
    private String title;
    private String message;
    private UserResponse user;
}
