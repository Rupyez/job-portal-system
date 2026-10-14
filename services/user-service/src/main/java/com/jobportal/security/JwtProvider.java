
package com.jobportal.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class JwtProvider {

    private static final int MIN_KEY_BYTES = 32;

    private static final String CLAIM_TOKEN_TYPE = "typ";
    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_AUTHORITIES = "auth";

    private final JwtProperties properties;
    private final SecretKey signingKey;
    private final JwtParser parser;
    private final Clock clock;

    // Initializes the JWT signing key and parser.
    public JwtProvider(JwtProperties properties) {
        this.properties = Objects.requireNonNull(
                properties, "JwtProperties must not be null");

        this.clock = Clock.systemUTC();

        String secret = Objects.requireNonNull(
                properties.secret(), "JWT secret must not be null");

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalArgumentException(
                    "JWT secret must contain at least 32 UTF-8 bytes");
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);

        this.parser = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.issuer())
                .clock(() -> Date.from(clock.instant()))
                .clockSkewSeconds(properties.clockSkew().toSeconds())
                .build();

        log.info(
                "JwtProvider initialized. Issuer={}, access TTL={}, refresh TTL={}",
                properties.issuer(),
                properties.accessTokenTtl(),
                properties.refreshTokenTtl()
        );
    }

    // Generates a short-lived access token with user authorities.
    public IssuedToken generateAccessToken(
            String subject,
            String userId,
            Collection<String> authorities) {

        return issue(
                subject,
                userId,
                authorities,
                TokenType.ACCESS,
                properties.accessTokenTtl()
        );
    }

    // Generates a refresh token without authorities.
    public IssuedToken generateRefreshToken(
            String subject,
            String userId) {

        return issue(
                subject,
                userId,
                List.of(),
                TokenType.REFRESH,
                properties.refreshTokenTtl()
        );
    }

    // Shared implementation for generating both token types.
    private IssuedToken issue(
            String subject,
            String userId,
            Collection<String> authorities,
            TokenType tokenType,
            java.time.Duration ttl) {

        Objects.requireNonNull(subject, "Subject must not be null");
        Objects.requireNonNull(userId, "User ID must not be null");
        Objects.requireNonNull(authorities, "Authorities must not be null");
        Objects.requireNonNull(tokenType, "Token type must not be null");
        Objects.requireNonNull(ttl, "Token TTL must not be null");

        if (subject.isBlank() || userId.isBlank()) {
            throw new IllegalArgumentException(
                    "Subject and user ID must not be blank");
        }

        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException(
                    "Token TTL must be positive");
        }

        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(ttl);

        var builder = Jwts.builder()
                .issuer(properties.issuer())
                .subject(subject)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim(CLAIM_TOKEN_TYPE, tokenType.name())
                .claim(CLAIM_USER_ID, userId);

        if (tokenType == TokenType.ACCESS) {
            builder.claim(
                    CLAIM_AUTHORITIES,
                    List.copyOf(authorities)
            );
        }

        String token = builder
                .signWith(signingKey)
                .compact();

        return new IssuedToken(
                token,
                tokenType,
                issuedAt,
                expiresAt
        );
    }

    // Parses a signed JWT and validates its signature, issuer, and expiration.
    public Claims parseClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT token must not be blank");
        }

        return parser.parseSignedClaims(token).getPayload();
    }

    // Checks whether the token is valid and has the expected token type.
    public boolean isTokenValid(
            String token,
            TokenType expectedType) {

        if (expectedType == null) {
            return false;
        }

        try {
            Claims claims = parseClaims(token);

            String tokenType = claims.get(
                    CLAIM_TOKEN_TYPE, String.class);

            String userId = claims.get(
                    CLAIM_USER_ID, String.class);

            return expectedType.name().equals(tokenType)
                    && claims.getSubject() != null
                    && !claims.getSubject().isBlank()
                    && userId != null
                    && !userId.isBlank();

        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    // Extracts the subject, typically the user's email.
    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    // Extracts the user's ID.
    public String extractUserId(String token) {
        return parseClaims(token).get(
                CLAIM_USER_ID, String.class);
    }

    // Extracts authorities from an access token.
    public List<String> extractAuthorities(String token) {
        Claims claims = parseClaims(token);

        List<?> authorities = claims.get(
                CLAIM_AUTHORITIES, List.class);

        if (authorities == null) {
            return List.of();
        }

        return authorities.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    // Distinguishes access tokens from refresh tokens.
    public enum TokenType {
        ACCESS,
        REFRESH
    }

    // Details returned when a token is issued.
    public record IssuedToken(
            String value,
            TokenType type,
            Instant issuedAt,
            Instant expiresAt) {
    }
}
