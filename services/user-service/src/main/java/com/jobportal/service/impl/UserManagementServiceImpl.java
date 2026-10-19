
package com.jobportal.service.impl;

import com.jobportal.mapper.UserMapper;
import com.jobportal.modal.RefreshToken;
import com.jobportal.modal.User;
import com.jobportal.payload.UpdateProfileRequest;
import com.jobportal.repository.RefreshTokenRepository;
import com.jobportal.repository.UserRepository;
import com.jobportal.service.UserManagementService;
import domain.UserStatus;
import dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyProfile(String email) {
        User user = findActiveUser(email);
        return UserMapper.toDTO(user);
    }

    @Override
    @Transactional
    public UserResponse updateMyProfile(
            String email,
            UpdateProfileRequest request) {

        User user = findActiveUser(email);

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());

        user.setPhoneNumber(
                request.getPhoneNumber() == null
                        ? null
                        : request.getPhoneNumber().trim()
        );

        user.setProfileImage(
                request.getProfileImage() == null
                        ? null
                        : request.getProfileImage().trim()
        );

        User savedUser = userRepository.save(user);

        return UserMapper.toDTO(savedUser);
    }

    private User findActiveUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User profile not found"
                ));

        if (!Boolean.TRUE.equals(user.getEnabled())
                || user.getUserStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This account is not active"
            );
        }

        return user;
    }


    @Override
    @Transactional
    public void deleteMyProfile(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found"));

        //prevent repeated deletion
            if(Boolean.FALSE.equals(user.getEnabled()) || user.getUserStatus() == UserStatus.DELETED){
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found");
            }

            //soft-delete the account while preserving its database record.
            user.setEnabled(false);
            user.setUserStatus(UserStatus.DELETED);
            user.setDeletedAt(LocalDateTime.now());

            userRepository.save(user);

            //Revoke all active refresh token
        List<RefreshToken> activeToken = refreshTokenRepository.findAllByUser_IdAndRevokedAtIsNull(user.getId());

        Instant revokedAt = Instant.now();
        activeToken.forEach(token -> token.setRevokedAt(revokedAt));
        refreshTokenRepository.saveAll(activeToken);
    }
}
