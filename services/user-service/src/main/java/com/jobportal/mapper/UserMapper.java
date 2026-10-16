package com.jobportal.mapper;

import com.jobportal.modal.User;
import dto.response.UserResponse;

public final class UserMapper {

    private UserMapper() {
        // Utility class; prevent instantiation.
    }


    public static UserResponse toDTO(User user){

        if (user == null) {
            return null;
        }


        UserResponse dto = new UserResponse();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName (user.getFirstName());
        dto.setLastName (user.getLastName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setProfileImage(user.getProfileImage());
        dto.setRole(user.getRole());
        dto.setUserStatus (user.getUserStatus());
        dto.setLastLogin(user.getLastLogin());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setEnabled(user.getEnabled());

        return dto;
    }


}
