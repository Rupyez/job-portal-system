package com.jobportal.service;

import com.jobportal.payload.AuthResponse;
import com.jobportal.payload.LoginRequest;
import com.jobportal.payload.SignupRequest;

public interface AuthService {

    AuthResponse signup(SignupRequest req);
    AuthResponse login(LoginRequest req);
    AuthResponse refresh(String refreshToken);
    void logout(String refreshToken);
}
