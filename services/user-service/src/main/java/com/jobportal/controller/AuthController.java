package com.jobportal.controller;

import com.jobportal.payload.*;
import com.jobportal.service.AuthService;
import com.jobportal.service.PasswordResetService;
import com.jobportal.service.UserManagementService;
import dto.response.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final UserManagementService userManagementService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody @Valid SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    /**
     * Revokes the supplied refresh token. The client should discard its local tokens.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    /**
     * Always returns the same message, whether or not the email is registered.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        String message = passwordResetService.requestPasswordReset(request);
        return ResponseEntity.ok(Map.of("message", message));
    }

    /**
     * Accepts a valid, unexpired, single-use reset token and a new password.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message",
                "Password has been reset. Please log in with your new password."));
    }

    /**
     * Changes the password for the currently authenticated user.
     * Existing refresh-token sessions are revoked after the change.
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(Map.of("message",
                "Password changed. Please log in again on your devices."));
    }


    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(Authentication authentication){
        UserResponse response = userManagementService.getMyProfile(authentication.getName());
        return ResponseEntity.ok(response);
    }

    // PUT: Update the currently logged-in user's profile.
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(Authentication authentication, @Valid @RequestBody UpdateProfileRequest request){
        UserResponse response = userManagementService.updateMyProfile(authentication.getName(), request);

        return ResponseEntity.ok(response);
    }


    // DELETE: Soft-delete the currently logged-in user's profile.
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyProfile(Authentication authentication){
        userManagementService.deleteMyProfile(authentication.getName());

        return ResponseEntity.noContent().build();
    }
}
