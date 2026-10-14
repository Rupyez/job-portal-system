package com.jobportal.security;


import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        String issuer,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        Duration clockSkew
) {

    public JwtProperties {
        if (issuer == null || issuer.isBlank()){
            throw new IllegalArgumentException("JWT issuer must not be blank");
        }


        if (accessTokenTtl == null || accessTokenTtl.isZero() || accessTokenTtl.isNegative()){
            throw new IllegalArgumentException("Access token TTL must be positive");
        }

        if (refreshTokenTtl == null
                || refreshTokenTtl.isZero()
                || refreshTokenTtl.isNegative()) {
            throw new IllegalArgumentException(
                    "Refresh token TTL must be positive");
        }

        if (clockSkew == null
                || clockSkew.isNegative()) {
            throw new IllegalArgumentException(
                    "JWT clock skew cannot be negative");
        }
    }
}
