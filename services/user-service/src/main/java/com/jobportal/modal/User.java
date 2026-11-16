package com.jobportal.modal;


import domain.UserRole;
import domain.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)


public class User {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column
    private String password;

    @Column(name = "oauth_provider", length = 30)
    private String oauthProvider;

    @Column(name = "oauth_subject", unique = true, length = 255)
    private String oauthSubject;

    @Column(length = 20)
    private String phoneNumber;

    private String profileImage;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus userStatus = UserStatus.ACTIVE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role=UserRole.ROLE_JOB_SEEKER;

    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastLogin;

    private LocalDateTime suspendedAt;

    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (enabled == null) {
            enabled = true;
            if (userStatus == null) userStatus = UserStatus.ACTIVE;
            if (role == null) role = UserRole.ROLE_JOB_SEEKER;
        }


    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


    // ---- Domain helpers (use these in the OAuth2 service) ----
    public String displayName(){
        String first = firstName == null ? "" : firstName;
        String last  = lastName  == null ? "" : lastName;
        return (first + " " + last).trim();
    }


    public void linkGoogle(String subject, String firstName, String lastName, String imageUrl){
        this.oauthProvider = "google";
        this.oauthSubject  = subject;
        if (firstName != null && !firstName.isBlank()) this.firstName = firstName;
        if (lastName  != null && !lastName.isBlank())  this.lastName  = lastName;
        if (imageUrl  != null && !imageUrl.isBlank())  this.profileImage = imageUrl;
    }


    public void markLogin() {
        this.lastLogin = LocalDateTime.now();
    }
}
