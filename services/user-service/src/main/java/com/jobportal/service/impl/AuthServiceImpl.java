package com.jobportal.service.impl;

import com.jobportal.exception.EmailAlreadyExistsException;
import com.jobportal.mapper.UserMapper;
import com.jobportal.modal.User;
import com.jobportal.payload.AuthResponse;
import com.jobportal.payload.LoginRequest;
import com.jobportal.payload.SignupRequest;
import com.jobportal.repository.UserRepository;
import com.jobportal.service.AuthService;
import domain.UserRole;
import dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

            UserResponse userResponse = new UserResponse();
            userResponse.setId(savedUser.getId());
            userResponse.setFirstName(savedUser.getFirstName());
            userResponse.setLastName(savedUser.getLastName());

            userResponse.setEmail(savedUser.getEmail());
            userResponse.setPhoneNumber(savedUser.getPhoneNumber());
            userResponse.setRole(savedUser.getRole());

            AuthResponse response = new AuthResponse();
            response.setTitle("Welcome " +savedUser.getFirstName());
            response.setMessage("Signup successful.");
            response.setJwt("jwt");
            response.setUser(UserMapper.toDTO(savedUser));

            return response;

        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyExistsException(
                    "An account with this email already exists."
            );
        }

    }

    private String normalizeEmail(String email){
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public AuthResponse login(LoginRequest req) {
        return null;
    }
}
