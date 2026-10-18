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
