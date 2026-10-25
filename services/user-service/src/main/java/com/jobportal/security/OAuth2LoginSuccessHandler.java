package com.jobportal.security;

import com.jobportal.modal.OAuth2AuthorizationCode;
import com.jobportal.repository.OAuth2AuthorizationCodeRepository;
import com.jobportal.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final OAuth2AuthorizationCodeRepository codeRepository;

    @Value("${app.frontend-base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String subject = principal.getAttribute("sub");

        var user = userRepository.findByOauthSubject(subject)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated Google identity was not linked to a user"));

        String rawCode = OneTimeCodeUtil.generate();
        OAuth2AuthorizationCode code = new OAuth2AuthorizationCode();
        code.setUser(user);
        code.setCodeHash(OneTimeCodeUtil.sha256(rawCode));
        code.setExpiresAt(Instant.now().plus(2, ChronoUnit.MINUTES));
        codeRepository.save(code);

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendBaseUrl)
                .path("/oauth2/callback")
                .queryParam("code", rawCode)
                .build()
                .encode()
                .toUriString();

        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Cache-Control", "no-store");
        response.sendRedirect(redirectUrl);
    }
}