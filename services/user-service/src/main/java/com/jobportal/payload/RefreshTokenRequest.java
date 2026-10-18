package com.jobportal.payload;


import jakarta.validation.constraints.NotBlank;

/**
 * Receives a refresh token from the client.
 *
 * Used by the refresh and logout API endpoints.
 */

public class RefreshTokenRequest {

    // The client must provide a non-null, non-empty token.
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    // Reads the refresh token from the request.
    public String getRefreshToken() {
        return refreshToken;
    }

    //Allows Spring/Jackson to populate the token from JSON
    public void setRefreshToken(String refreshToken){
        this.refreshToken = refreshToken;
    }
}
