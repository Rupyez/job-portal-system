package com.jobportal.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.User;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private CustomUserDetailService userDetailService;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtProvider, userDetailService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        // SecurityContextHolder is thread-local; clear it so tests cannot leak
        // authentication state into another test running on the same thread.
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueWithoutAuthenticatingWhenAuthorizationHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/private");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtProvider, userDetailService);
    }

    @Test
    void shouldNotAuthenticateWhenAccessTokenIsInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtProvider.isTokenValid("invalid-token", JwtProvider.TokenType.ACCESS))
                .thenReturn(false);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtProvider).isTokenValid("invalid-token", JwtProvider.TokenType.ACCESS);
        verifyNoInteractions(userDetailService);
    }

    @Test
    void shouldAuthenticateEnabledUserWhenAccessTokenIsValid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        UserDetails userDetails = User.withUsername("rupesh@example.com")
                .password("not-used-by-this-filter")
                .authorities("ROLE_JOB_SEEKER")
                .build();

        when(jwtProvider.isTokenValid("valid-access-token", JwtProvider.TokenType.ACCESS))
                .thenReturn(true);
        when(jwtProvider.extractSubject("valid-access-token"))
                .thenReturn("rupesh@example.com");
        when(userDetailService.loadUserByUsername("rupesh@example.com"))
                .thenReturn(userDetails);

        filter.doFilter(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("rupesh@example.com", authentication.getName());
        assertEquals("ROLE_JOB_SEEKER", authentication.getAuthorities().iterator().next().getAuthority());

        verify(userDetailService).loadUserByUsername("rupesh@example.com");
    }

    @Test
    void shouldNotAuthenticateDisabledUserEvenWhenAccessTokenIsValid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        UserDetails disabledUser = User.withUsername("rupesh@example.com")
                .password("not-used-by-this-filter")
                .authorities("ROLE_JOB_SEEKER")
                .disabled(true)
                .build();

        when(jwtProvider.isTokenValid("valid-access-token", JwtProvider.TokenType.ACCESS))
                .thenReturn(true);
        when(jwtProvider.extractSubject("valid-access-token"))
                .thenReturn("rupesh@example.com");
        when(userDetailService.loadUserByUsername("rupesh@example.com"))
                .thenReturn(disabledUser);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldNotAuthenticateWhenRefreshTokenIsPresentedAsBearerToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer refresh-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtProvider.isTokenValid("refresh-token", JwtProvider.TokenType.ACCESS))
                .thenReturn(false);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(userDetailService);
    }
}
