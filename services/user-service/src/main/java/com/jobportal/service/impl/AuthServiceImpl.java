package com.jobportal.service.impl;

import com.jobportal.exception.EmailAlreadyExistsException;
import com.jobportal.mapper.UserMapper;
import com.jobportal.modal.RefreshToken;
import com.jobportal.modal.User;
import com.jobportal.payload.AuthResponse;
import com.jobportal.payload.ChangePasswordRequest;
import com.jobportal.payload.LoginRequest;
import com.jobportal.payload.SignupRequest;
import com.jobportal.repository.RefreshTokenRepository;
import com.jobportal.repository.UserRepository;
import com.jobportal.security.JwtProvider;
import com.jobportal.security.TokenHashUtil;
import com.jobportal.service.AuthService;
import domain.UserRole;
import domain.UserStatus;
import dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest req) {
        Objects.requireNonNull(req, "Signup request must not be null");

        String email = normalizeEmail(req.getEmail());

        // Check whether the email is already registered
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(
                    "An account with this email already exists: " + req.getEmail()
            );
        }

        User user = User.builder()
                .firstName(req.getFirstName().trim())
                .lastName(req.getLastName().trim())
                .email(email)
                .password(passwordEncoder.encode(req.getPassword()))
                .role(UserRole.ROLE_JOB_SEEKER)
                .phoneNumber(req.getPhoneNumber())
                .build();


        final User savedUser;
        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // The database unique constraint protects against two concurrent
            // signup requests using the same normalized email.
            throw new EmailAlreadyExistsException(
                    "An account with this email already exists."
            );
        }

        // Keep token persistence outside the duplicate-email catch block so
        // token-table failures are not incorrectly reported as email conflicts.
        return createAuthResponse(
                savedUser,
                "Welcome " + savedUser.getFirstName(),
                "Registration successful"
        );

    }

    private AuthResponse createAuthResponse(User user, String title, String message) {
        String role = user.getRole().name();
        String userId = user.getId().toString();

        // Create one access token for API requests.
        JwtProvider.IssuedToken accessToken = jwtProvider.generateAccessToken(
                user.getEmail(),
                userId,
                List.of(role)
        );

        // Create exactly one refresh token. This same token must be returned
        // to the client and hashed for storage, otherwise refresh lookup fails.
        JwtProvider.IssuedToken refreshToken = jwtProvider.generateRefreshToken(
                user.getEmail(),
                userId
        );

        // Store only the SHA-256 hash, never the raw refresh token.
        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setUser(user);
        refreshTokenEntity.setTokenHash(TokenHashUtil.sha256(refreshToken.value()));
        refreshTokenEntity.setExpiresAt(refreshToken.expiresAt());
        refreshTokenRepository.save(refreshTokenEntity);

        // Return the raw tokens to the client.
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

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isAccountActive(User user) {
        return Boolean.TRUE.equals(user.getEnabled())
                && user.getUserStatus() == UserStatus.ACTIVE;
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest req) {
        Objects.requireNonNull(req, "Login request must not be null.");

        String email = normalizeEmail(req.getEmail());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        //Use the same Bcrypt encoder that was used during registration
        if(!passwordEncoder.matches(req.getPassword(), user.getPassword())){
            throw new BadCredentialsException("Invalid email or password");

        }

        if (!isAccountActive(user)) {
            throw new BadCredentialsException(
                    "This account is disabled or inactive."
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


    @Override
    @Transactional
    public AuthResponse refresh(String refreshToken) {

        // STEP 1: Validate the refresh token.
        // This checks the JWT signature, expiration, issuer, and token type.
        // A refresh token must not be an access token.

        if(!jwtProvider.isTokenValid(refreshToken, JwtProvider.TokenType.REFRESH)){
            throw new BadCredentialsException("Invalid Refresh Token");
        }

        //STEP 2: Hash the token received from the client
        //We store only the hash in database, not the original token.
        String tokenHash = TokenHashUtil.sha256(refreshToken);

        //STEP 3: Find the matching token record in the database.
        //If no record exists, the token was never stored or is not recognized.
        // Lock this row until the transaction finishes. If two requests try
        // to rotate the same token, the second request sees it as revoked.
        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));


        //STEP 4: Check whether the stored token is expired or revoked
        if(storedToken.isRevoked() || storedToken.isExpired()){
            throw new BadCredentialsException("Refresh token is expired or revoked.");
        }


        //STEP 5: Get the user associated with this refresh token.
        User user = storedToken.getUser();

        //STEp 6: Confirm the token belongs to the expected user.
        //compare the user id and email inside the jwt with the database user.
        if(!user.getId().toString().equals(
                jwtProvider.extractUserId(refreshToken)) || !user.getEmail().equals(jwtProvider.extractSubject(refreshToken))){
            throw new BadCredentialsException("Invalid refresh token");
        }



        //STEP 7: Do not issue tokens to a disabled account
        if (!isAccountActive(user)) {
            throw new BadCredentialsException("Account is disabled or inactive");
        }

        //STEP 8: Revoke the old refresh token
        //This prevents the same token from being reused after rotation
        storedToken.setRevokedAt(Instant.now());



        return createAuthResponse(user, "Session refreshed", "Authentication refreshed successfully");

    }


    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        String normalizedEmail = normalizeEmail(email);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials."));

        if (!isAccountActive(user)) {
            throw new BadCredentialsException("Invalid credentials.");
        }

        // Verify the current password before allowing a credential change.
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials.");
        }

        // Avoid a pointless password change to the same password.
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("The new password must differ from the current password.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // A password change invalidates refresh sessions on every device.
        List<RefreshToken> activeTokens =
                refreshTokenRepository.findAllByUser_IdAndRevokedAtIsNull(user.getId());
        Instant revokedAt = Instant.now();
        activeTokens.forEach(token -> token.setRevokedAt(revokedAt));
        refreshTokenRepository.saveAll(activeTokens);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
       // flow of the code
//        1. Client requests logout
//        Sends the refresh token to the backend.
//
//        2. Validate the input
//        Reject a missing or blank token.
//
//        3. Hash and find the token
//        Look up the hash in the database.
//
//        4. Revoke the token
//        Set revokedAt if the record exists and is active.
//
//        5. Commit the transaction
//        The refresh token can no longer be used to renew the session.


        //STEP 1: check whether the client sent a refresh token.
        // A null or blank token can identify a session to revoke.
        if(refreshToken == null || refreshToken.isBlank()){
            throw new BadCredentialsException("Refresh token is required.");
        }

        // STEP 2: Hash the refresh token.
        // We store the token's SHA-256 hash in the database rather
        // than storing the original token in plain text.
        String tokenHash = TokenHashUtil.sha256(refreshToken);


        // STEP 3: Search the database for this refresh token.
        // findByTokenHash() returns an Optional because the record
        // might not exist.
        refreshTokenRepository.findByTokenHashForUpdate(tokenHash).ifPresent(storedToken ->{

            //check whether the token has already been revoked
            if(!storedToken.isRevoked()){
                storedToken.setRevokedAt(Instant.now());
            }
        });



    }
}
