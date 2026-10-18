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
import com.jobportal.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final String GENERIC_REQUEST_MESSAGE =
            "If an account exists for that email, password reset instructions will be sent.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.frontend-base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Value("${app.security.password-reset-token-ttl:15m}")
    private Duration resetTokenTtl;

    @Value("${spring.mail.username:no-reply@example.com}")
    private String fromAddress;

    @Override
    @Transactional
    public String requestPasswordReset(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        // Same response for existing and unknown emails prevents account enumeration.
        userRepository.findByEmail(email).ifPresent(user -> {
            Instant now = Instant.now();

            // A newly requested reset invalidates any older, unused reset links.
            List<PasswordResetToken> previousTokens =
                    passwordResetTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId());
            previousTokens.forEach(token -> token.setUsedAt(now));
            passwordResetTokenRepository.saveAll(previousTokens);

            String rawToken = generateSecureToken();

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(user);
            resetToken.setTokenHash(TokenHashUtil.sha256(rawToken));
            resetToken.setExpiresAt(now.plus(resetTokenTtl));
            passwordResetTokenRepository.save(resetToken);

            sendResetEmail(user, rawToken);
        });

        return GENERIC_REQUEST_MESSAGE;
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String rawToken = request.getToken();

        if (rawToken == null || rawToken.isBlank()) {
            throw new BadCredentialsException("Invalid or expired password reset token.");
        }

        String tokenHash = TokenHashUtil.sha256(rawToken);

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new BadCredentialsException(
                        "Invalid or expired password reset token."));

        if (resetToken.isUsed() || resetToken.isExpired()) {
            throw new BadCredentialsException("Invalid or expired password reset token.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark the reset token as used so it cannot be replayed.
        resetToken.setUsedAt(Instant.now());

        // Password reset should sign out existing sessions on every device.
        List<RefreshToken> activeRefreshTokens =
                refreshTokenRepository.findAllByUser_IdAndRevokedAtIsNull(user.getId());
        Instant revokedAt = Instant.now();
        activeRefreshTokens.forEach(token -> token.setRevokedAt(revokedAt));
        refreshTokenRepository.saveAll(activeRefreshTokens);
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private void sendResetEmail(User user, String rawToken) {
        String resetUrl = frontendBaseUrl.replaceAll("/+$", "")
                + "/reset-password?token=" + rawToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(user.getEmail());
        message.setSubject("Reset your Job Portal password");
        message.setText("""
                Hello %s,

                We received a request to reset the password for your Job Portal account.
                Use the link below within %d minutes. This link can only be used once.

                %s

                If you did not request this, you can ignore this email.
                """.formatted(user.getFirstName(), resetTokenTtl.toMinutes(), resetUrl));

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            // Do not log the reset token or expose whether the account exists.
            // In production, use a durable email queue and alert on delivery failures.
            log.error("Unable to send password reset email");
        }
    }
}
