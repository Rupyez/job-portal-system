package com.jobportal.service;

import com.jobportal.payload.UpdateProfileRequest;
import dto.response.UserResponse;

public interface UserManagementService {

    UserResponse getMyProfile(String email);
    UserResponse updateMyProfile(String email, UpdateProfileRequest request);
    void deleteMyProfile(String email);

}
