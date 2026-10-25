package com.jobportal.security;

import com.jobportal.modal.User;
import com.jobportal.repository.UserRepository;
import domain.UserRole;
import domain.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User googleUser = super.loadUser(request);

        if (!"google".equalsIgnoreCase(request.getClientRegistration().getRegistrationId())) {
            throw oauthError("unsupported_provider", "Only Google OAuth2 login is supported.");
        }

        Map<String, Object> attributes = googleUser.getAttributes();
        String subject = stringAttribute(attributes, "sub");
        String rawEmail = stringAttribute(attributes, "email");
        Object verified = attributes.get("email_verified");

        if (subject == null || rawEmail == null || !Boolean.TRUE.equals(verified)) {
            throw oauthError("invalid_google_identity",
                    "Google must provide a verified email address.");
        }

        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(email).orElseGet(() -> createGoogleUser(attributes, email, subject));

        if (user.getOauthSubject() != null && !user.getOauthSubject().equals(subject)) {
            throw oauthError("identity_conflict",
                    "This email is already linked to a different Google account.");
        }

        if (user.getOauthProvider() != null
                && !"google".equalsIgnoreCase(user.getOauthProvider())) {
            throw oauthError("provider_conflict",
                    "This account is linked to a different identity provider.");
        }

        if (!Boolean.TRUE.equals(user.getEnabled()) || user.getUserStatus() != UserStatus.ACTIVE) {
            throw oauthError("account_inactive", "This account is disabled or inactive.");
        }

        user.setOauthProvider("google");
        user.setOauthSubject(subject);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority(user.getRole().name())),
                attributes,
                "sub");
    }

    private User createGoogleUser(Map<String, Object> attributes, String email, String subject) {
        String givenName = stringAttribute(attributes, "given_name");
        String familyName = stringAttribute(attributes, "family_name");
        String fullName = stringAttribute(attributes, "name");

        if ((givenName == null || givenName.isBlank()) && fullName != null) {
            String[] parts = fullName.trim().split("\\s+", 2);
            givenName = parts[0];
            if ((familyName == null || familyName.isBlank()) && parts.length > 1) {
                familyName = parts[1];
            }
        }

        if (givenName == null || givenName.isBlank()) {
            givenName = "Google";
        }
        if (familyName == null || familyName.isBlank()) {
            familyName = "User";
        }

        User user = User.builder()
                .firstName(givenName.trim())
                .lastName(familyName.trim())
                .email(email)
                .password(null)
                .oauthProvider("google")
                .oauthSubject(subject)
                .role(UserRole.ROLE_JOB_SEEKER)
                .userStatus(UserStatus.ACTIVE)
                .enabled(true)
                .profileImage(stringAttribute(attributes, "picture"))
                .build();

        return userRepository.saveAndFlush(user);
    }

    private String stringAttribute(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value instanceof String string && !string.isBlank() ? string : null;
    }

    private OAuth2AuthenticationException oauthError(String code, String description) {
        return new OAuth2AuthenticationException(new OAuth2Error(code, description, null));
    }
}