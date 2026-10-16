package com.jobportal.service.impl;

import com.jobportal.exception.EmailAlreadyExistsException;
import com.jobportal.mapper.UserMapper;
import com.jobportal.modal.User;
import com.jobportal.payload.AuthResponse;
import com.jobportal.payload.LoginRequest;
import com.jobportal.payload.SignupRequest;
import com.jobportal.repository.UserRepository;
import com.jobportal.security.JwtProvider;
import com.jobportal.service.AuthService;
import domain.UserRole;
import dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest req) {
        Objects.requireNonNull(req, "Signup request must not be null");

        String email = normalizeEmail(req.getEmail());

        // Check whether the email is already registered
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new EmailAlreadyExistsException(
                    "An account with this email already exists: " + req.getEmail()
            );
        }

        // Prevent users from assigning themselves privileged roles
//        if (req.getRole() == UserRole.ADMIN) {
//            throw new IllegalArgumentException(
//                    "Cannot self-register with the ADMIN role."
//            );
//        }

        User user = User.builder()
                .firstName(req.getFirstName().trim())
                .lastName(req.getLastName().trim())
                .email(email)
                .password(passwordEncoder.encode(req.getPassword()))
                .role(UserRole.ROLE_JOB_SEEKER)
                .phoneNumber(req.getPhoneNumber())
                .build();


        try {
            User savedUser = userRepository.saveAndFlush(user);

            return createAuthResponse(savedUser, "Welcome " + savedUser.getFirstName(), "Registration successful");

        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyExistsException(
                    "An account with this email already exists."
            );
        }

    }

    private AuthResponse createAuthResponse(User user, String title, String message){
        String role = user.getRole().name();

        JwtProvider.IssuedToken accessToken = jwtProvider.generateAccessToken(
                user.getEmail(),
                user.getId().toString(),
                List.of(role));

        JwtProvider.IssuedToken refreshToken = jwtProvider.generateRefreshToken(
                user.getEmail(),
                user.getId().toString()
        );

        AuthResponse response = new AuthResponse();
        response.setJwt(accessToken.value());
        response.setRefreshToken(refreshToken.value());
        response.setAccessTokenExpiresAt(accessToken.expiresAt());
        response.setRefreshTokenExpiresAt(refreshToken.expiresAt());
        response.setTitle(title);
        response.setMessage(message);
        response.setUser(UserMapper.toDTO(user));

        return response;
    }

    private String normalizeEmail(String email){
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public AuthResponse login(LoginRequest req) {
        Objects.requireNonNull(req, "Login request must not be null.");

        String email = normalizeEmail(req.getEmail());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        //Use the same Bcrypt encoder that was used during registration
        if(!passwordEncoder.matches(req.getPassword(), user.getPassword())){
            throw new BadCredentialsException("Invalid email or password");

        }

        if(!Boolean.TRUE.equals(user.getEnabled())){
            throw new BadCredentialsException(
                    "This account is not enabled."
            );
        }

        user.setLastLogin(LocalDateTime.now());
        User updatedUser = userRepository.saveAndFlush(user);

        return createAuthResponse(
                updatedUser,
                "Welcome back " + updatedUser.getFirstName(),
                "Login successful."
        );
    }
}
