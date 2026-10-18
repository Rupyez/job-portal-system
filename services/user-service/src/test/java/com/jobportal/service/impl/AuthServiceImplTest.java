package com.jobportal.service.impl;

import com.jobportal.exception.EmailAlreadyExistsException;
import com.jobportal.modal.RefreshToken;
import com.jobportal.modal.User;
import com.jobportal.payload.AuthResponse;
import com.jobportal.payload.LoginRequest;
import com.jobportal.payload.SignupRequest;
import com.jobportal.repository.RefreshTokenRepository;
import com.jobportal.repository.UserRepository;
import com.jobportal.security.JwtProvider;
import com.jobportal.security.TokenHashUtil;
import com.jobportal.security.TokenHashUtil;
import domain.UserRole;
import domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id(100L)
                .firstName("Rupesh")
                .lastName("Duwal")
                .email("rupesh@example.com")
                .password("bcrypt-hash")
                .role(UserRole.ROLE_JOB_SEEKER)
                .userStatus(UserStatus.ACTIVE)
                .enabled(true)
                .build();
    }

    @Test
    void signupShouldRejectAnEmailThatAlreadyExists() {
        SignupRequest request = new SignupRequest();
        request.setFirstName("Rupesh");
        request.setLastName("Duwal");
        request.setEmail("Rupesh@Example.com");
        request.setPassword("strong-password");

        when(userRepository.existsByEmail("rupesh@example.com")).thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.signup(request)
        );

        verify(userRepository).existsByEmail("rupesh@example.com");
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verifyNoInteractions(jwtProvider, refreshTokenRepository);
    }

    @Test
    void loginShouldReturnTokensWhenCredentialsAreValid() {
        LoginRequest request = loginRequest("  RUPESH@EXAMPLE.COM  ", "correct-password");

        Instant now = Instant.now();
        JwtProvider.IssuedToken accessToken = new JwtProvider.IssuedToken(
                "mock-access-token",
                JwtProvider.TokenType.ACCESS,
                now,
                now.plusSeconds(900)
        );
        JwtProvider.IssuedToken refreshToken = new JwtProvider.IssuedToken(
                "mock-refresh-token",
                JwtProvider.TokenType.REFRESH,
                now,
                now.plusSeconds(604800)
        );

        when(userRepository.findByEmail("rupesh@example.com"))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("correct-password", "bcrypt-hash"))
                .thenReturn(true);
        when(userRepository.saveAndFlush(activeUser)).thenReturn(activeUser);
        when(jwtProvider.generateAccessToken(
                "rupesh@example.com",
                "100",
                List.of("ROLE_JOB_SEEKER")
        )).thenReturn(accessToken);
        when(jwtProvider.generateRefreshToken("rupesh@example.com", "100"))
                .thenReturn(refreshToken);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.login(request);

        // The service normalizes the email, verifies the password, and returns both tokens.
        org.junit.jupiter.api.Assertions.assertEquals("mock-access-token", response.getJwt());
        org.junit.jupiter.api.Assertions.assertEquals("mock-refresh-token", response.getRefreshToken());
        org.junit.jupiter.api.Assertions.assertEquals("Welcome back Rupesh", response.getTitle());
        org.junit.jupiter.api.Assertions.assertEquals("Login successful.", response.getMessage());
        org.junit.jupiter.api.Assertions.assertEquals("rupesh@example.com", response.getUser().getEmail());

        verify(userRepository).findByEmail("rupesh@example.com");
        verify(passwordEncoder).matches("correct-password", "bcrypt-hash");
        verify(userRepository).saveAndFlush(activeUser);
        verify(jwtProvider).generateAccessToken(
                "rupesh@example.com",
                "100",
                List.of("ROLE_JOB_SEEKER")
        );
        verify(jwtProvider).generateRefreshToken("rupesh@example.com", "100");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void loginShouldRejectAnUnknownEmail() {
        LoginRequest request = loginRequest("missing@example.com", "strong-password");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(request));

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void loginShouldRejectAnIncorrectPassword() {
        LoginRequest request = loginRequest("rupesh@example.com", "wrong-password");
        when(userRepository.findByEmail("rupesh@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong-password", "bcrypt-hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));

        verify(userRepository, never()).saveAndFlush(any(User.class));
        verifyNoInteractions(jwtProvider, refreshTokenRepository);
    }

    @Test
    void loginShouldRejectASuspendedAccountEvenWhenEnabledFlagIsTrue() {
        LoginRequest request = loginRequest("rupesh@example.com", "correct-password");
        activeUser.setUserStatus(UserStatus.SUSPENDED);

        when(userRepository.findByEmail("rupesh@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("correct-password", "bcrypt-hash")).thenReturn(true);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));

        verify(userRepository, never()).saveAndFlush(any(User.class));
        verifyNoInteractions(jwtProvider, refreshTokenRepository);
    }

    @Test
    void refreshShouldRevokeOldTokenAndIssueANewTokenSet() {
        String oldRawRefreshToken = "old-refresh-token";
        String oldTokenHash = TokenHashUtil.sha256(oldRawRefreshToken);

        RefreshToken storedToken = new RefreshToken();
        storedToken.setId(55L);
        storedToken.setUser(activeUser);
        storedToken.setTokenHash(oldTokenHash);
        storedToken.setExpiresAt(Instant.now().plusSeconds(3600));

        Instant now = Instant.now();
        JwtProvider.IssuedToken newAccessToken = new JwtProvider.IssuedToken(
                "new-access-token",
                JwtProvider.TokenType.ACCESS,
                now,
                now.plusSeconds(900)
        );
        JwtProvider.IssuedToken newRefreshToken = new JwtProvider.IssuedToken(
                "new-refresh-token",
                JwtProvider.TokenType.REFRESH,
                now,
                now.plusSeconds(604800)
        );

        when(jwtProvider.isTokenValid(oldRawRefreshToken, JwtProvider.TokenType.REFRESH))
                .thenReturn(true);
        when(jwtProvider.extractUserId(oldRawRefreshToken)).thenReturn("100");
        when(jwtProvider.extractSubject(oldRawRefreshToken)).thenReturn("rupesh@example.com");
        when(refreshTokenRepository.findByTokenHashForUpdate(oldTokenHash))
                .thenReturn(Optional.of(storedToken));
        when(jwtProvider.generateAccessToken(
                "rupesh@example.com",
                "100",
                List.of("ROLE_JOB_SEEKER")
        )).thenReturn(newAccessToken);
        when(jwtProvider.generateRefreshToken("rupesh@example.com", "100"))
                .thenReturn(newRefreshToken);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.refresh(oldRawRefreshToken);

        // Rotation must revoke the old refresh token and return a fresh token pair.
        assertNotNull(storedToken.getRevokedAt());
        assertEquals("new-access-token", response.getJwt());
        assertEquals("new-refresh-token", response.getRefreshToken());
        assertEquals("Session refreshed", response.getTitle());
        assertEquals("Authentication refreshed successfully", response.getMessage());

        verify(refreshTokenRepository).findByTokenHashForUpdate(oldTokenHash);
        verify(jwtProvider).generateAccessToken(
                "rupesh@example.com",
                "100",
                List.of("ROLE_JOB_SEEKER")
        );
        verify(jwtProvider).generateRefreshToken("rupesh@example.com", "100");
        verify(refreshTokenRepository).save(argThat(token ->
                token.getUser().equals(activeUser)
                        && token.getTokenHash().equals(TokenHashUtil.sha256("new-refresh-token"))
                        && token.getExpiresAt().equals(newRefreshToken.expiresAt())
        ));
    }

    @Test
    void refreshShouldRejectAnInvalidJwtBeforeLookingUpItsDatabaseRecord() {
        when(jwtProvider.isTokenValid("not-a-valid-token", JwtProvider.TokenType.REFRESH))
                .thenReturn(false);

        assertThrows(
                BadCredentialsException.class,
                () -> authService.refresh("not-a-valid-token")
        );

        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void logoutShouldRevokeAnActiveRefreshToken() {
        String rawRefreshToken = "refresh-token-to-logout";
        String tokenHash = TokenHashUtil.sha256(rawRefreshToken);

        RefreshToken storedToken = new RefreshToken();
        storedToken.setId(77L);
        storedToken.setUser(activeUser);
        storedToken.setTokenHash(tokenHash);
        storedToken.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash))
                .thenReturn(Optional.of(storedToken));

        authService.logout(rawRefreshToken);

        assertNotNull(storedToken.getRevokedAt());
        verify(refreshTokenRepository).findByTokenHashForUpdate(tokenHash);
    }

    @Test
    void logoutShouldLeaveAnAlreadyRevokedRefreshTokenUnchanged() {
        String rawRefreshToken = "already-revoked-token";
        String tokenHash = TokenHashUtil.sha256(rawRefreshToken);
        Instant originalRevokedAt = Instant.now().minusSeconds(60);

        RefreshToken storedToken = new RefreshToken();
        storedToken.setId(78L);
        storedToken.setUser(activeUser);
        storedToken.setTokenHash(tokenHash);
        storedToken.setExpiresAt(Instant.now().plusSeconds(3600));
        storedToken.setRevokedAt(originalRevokedAt);

        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash))
                .thenReturn(Optional.of(storedToken));

        authService.logout(rawRefreshToken);

        assertEquals(originalRevokedAt, storedToken.getRevokedAt());
        verify(refreshTokenRepository).findByTokenHashForUpdate(tokenHash);
    }

    @Test
    void logoutShouldNotFailWhenRefreshTokenIsNotFound() {
        String rawRefreshToken = "unknown-refresh-token";
        String tokenHash = TokenHashUtil.sha256(rawRefreshToken);

        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash))
                .thenReturn(Optional.empty());

        // Logout is intentionally idempotent: an unknown token has nothing to revoke.
        assertDoesNotThrow(() -> authService.logout(rawRefreshToken));

        verify(refreshTokenRepository).findByTokenHashForUpdate(tokenHash);
    }

    @Test
    void logoutShouldRejectABlankRefreshToken() {
        assertThrows(BadCredentialsException.class, () -> authService.logout("  "));

        verifyNoInteractions(refreshTokenRepository);
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }
}
