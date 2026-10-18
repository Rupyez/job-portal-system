package com.jobportal.service.impl;

import com.jobportal.modal.PasswordResetToken;
import com.jobportal.modal.RefreshToken;
import com.jobportal.modal.User;
import com.jobportal.payload.ForgotPasswordRequest;
import com.jobportal.payload.ResetPasswordRequest;
import com.jobportal.repository.PasswordResetTokenRepository;
import com.jobportal.repository.RefreshTokenRepository;
import com.jobportal.repository.UserRepository;
import com.jobportal.security.TokenHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "frontendBaseUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(passwordResetService, "resetTokenTtl", java.time.Duration.ofMinutes(15));
        ReflectionTestUtils.setField(passwordResetService, "fromAddress", "no-reply@example.com");

        user = User.builder()
                .id(100L)
                .firstName("Rupesh")
                .lastName("Duwal")
                .email("rupesh@example.com")
                .password("old-hash")
                .build();
    }

    @Test
    void forgotPasswordShouldSendEmailAndStoreOnlyTokenHashForExistingEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(" RUPESH@EXAMPLE.COM ");

        when(userRepository.findByEmail("rupesh@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findAllByUser_IdAndUsedAtIsNull(100L))
                .thenReturn(List.of());
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String response = passwordResetService.requestPasswordReset(request);

        assertTrue(response.contains("If an account exists"));
        ArgumentCaptor<PasswordResetToken> tokenCaptor =
                ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertEquals(user, savedToken.getUser());
        assertNotNull(savedToken.getTokenHash());
        assertEquals(64, savedToken.getTokenHash().length());
        assertTrue(savedToken.getExpiresAt().isAfter(Instant.now()));

        ArgumentCaptor<SimpleMailMessage> emailCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(emailCaptor.capture());
        assertEquals("rupesh@example.com", emailCaptor.getValue().getTo()[0]);
        assertTrue(emailCaptor.getValue().getText().contains("/reset-password?token="));
        assertFalse(emailCaptor.getValue().getText().contains(savedToken.getTokenHash()));
    }

    @Test
    void forgotPasswordShouldReturnSameMessageForUnknownEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("unknown@example.com");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        String response = passwordResetService.requestPasswordReset(request);

        assertTrue(response.contains("If an account exists"));
        verifyNoInteractions(passwordResetTokenRepository, mailSender);
    }

    @Test
    void resetPasswordShouldChangePasswordAndRevokeExistingRefreshTokens() {
        String rawToken = "valid-reset-token";
        String tokenHash = TokenHashUtil.sha256(rawToken);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(Instant.now().plusSeconds(900));

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash("a".repeat(64));
        refreshToken.setExpiresAt(Instant.now().plusSeconds(3600));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(rawToken);
        request.setNewPassword("new-strong-password");

        when(passwordResetTokenRepository.findByTokenHashForUpdate(tokenHash))
                .thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("new-strong-password")).thenReturn("new-bcrypt-hash");
        when(refreshTokenRepository.findAllByUser_IdAndRevokedAtIsNull(100L))
                .thenReturn(List.of(refreshToken));

        passwordResetService.resetPassword(request);

        assertEquals("new-bcrypt-hash", user.getPassword());
        assertNotNull(resetToken.getUsedAt());
        assertNotNull(refreshToken.getRevokedAt());
        verify(userRepository).save(user);
        verify(refreshTokenRepository).saveAll(List.of(refreshToken));
    }

    @Test
    void resetPasswordShouldRejectUnknownToken() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("unknown-token");
        request.setNewPassword("new-strong-password");

        when(passwordResetTokenRepository.findByTokenHashForUpdate(
                TokenHashUtil.sha256("unknown-token")))
                .thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> passwordResetService.resetPassword(request));

        verifyNoInteractions(passwordEncoder, refreshTokenRepository);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPasswordShouldRejectExpiredToken() {
        String rawToken = "expired-reset-token";
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(TokenHashUtil.sha256(rawToken));
        resetToken.setExpiresAt(Instant.now().minusSeconds(1));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(rawToken);
        request.setNewPassword("new-strong-password");

        when(passwordResetTokenRepository.findByTokenHashForUpdate(resetToken.getTokenHash()))
                .thenReturn(Optional.of(resetToken));

        assertThrows(BadCredentialsException.class,
                () -> passwordResetService.resetPassword(request));

        verifyNoInteractions(passwordEncoder, refreshTokenRepository);
        verify(userRepository, never()).save(any(User.class));
    }
}
