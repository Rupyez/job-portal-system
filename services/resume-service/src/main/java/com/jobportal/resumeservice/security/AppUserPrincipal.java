package com.jobportal.resumeservice.security;


import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Authenticated principal for the resume service.
 * Carries the internal userId (Long) resolved from the Google identity.
 */

public record AppUserPrincipal(
        Long userId,
        String email,
        String name,
        Map<String, Object> attributes
        ) implements OAuth2User {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(){
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }


    @Override
    public String getName() {
        return email;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
