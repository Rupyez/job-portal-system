
package com.jobportal.payload;

import dto.response.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    // Short-lived token used to access protected APIs.
    private String jwt;

    // Longer-lived token intended for refreshing authentication.
    private String refreshToken;

    private Instant accessTokenExpiresAt;
    private Instant refreshTokenExpiresAt;

    private String title;
    private String message;
    private UserResponse user;
}
