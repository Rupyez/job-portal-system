package com.jobportal.modal;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_token_user_id", columnList = "user_id")
        }
)

@Data
@NoArgsConstructor

public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    // Store the SHA-256 hash, never the raw refresh token.
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;


    @Column(name = "revoked_at")
    private Instant revokedAt;


    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;


    @PrePersist
    protected void onCreate(){
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public boolean isRevoked(){
        return revokedAt != null;
    }

    public boolean isExpired(){
        return !expiresAt.isAfter(Instant.now());
    }

}
