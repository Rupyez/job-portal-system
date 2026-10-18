package com.jobportal.service;

import com.jobportal.payload.ForgotPasswordRequest;
import com.jobportal.payload.ResetPasswordRequest;

public interface PasswordResetService {

    // Always returns a generic message so callers cannot discover registered emails.
    String requestPasswordReset(ForgotPasswordRequest request);

    // Validates a single-use, time-limited token and updates the password.
    void resetPassword(ResetPasswordRequest request);
}
