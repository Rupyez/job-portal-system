package com.jobportal.controller;

import com.jobportal.exception.GlobalExceptionHandler;
import com.jobportal.payload.AuthResponse;
import com.jobportal.service.AuthService;
import com.jobportal.service.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @Test
    void loginShouldReturnOkAndAuthenticationResponse() throws Exception {
        when(authService.login(any())).thenReturn(AuthResponse.builder()
                .jwt("mock-access-token")
                .refreshToken("mock-refresh-token")
                .title("Welcome back")
                .message("Login successful.")
                .build());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "rupesh@example.com",
                                  "password": "correct-password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt").value("mock-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("mock-refresh-token"))
                .andExpect(jsonPath("$.message").value("Login successful."));

        verify(authService).login(any());
    }

    @Test
    void loginShouldReturnBadRequestWhenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "correct-password"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Validation should stop the request before it reaches the service.
    }

    @Test
    void loginShouldReturnUnauthorizedWhenCredentialsAreRejected() throws Exception {
        when(authService.login(any()))
                .thenThrow(new BadCredentialsException("Internal detail should not leak"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "rupesh@example.com",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid credentials or token."));
    }

    @Test
    void refreshShouldReturnNewTokens() throws Exception {
        when(authService.refresh("mock-refresh-token")).thenReturn(AuthResponse.builder()
                .jwt("new-access-token")
                .refreshToken("new-refresh-token")
                .message("Authentication refreshed successfully")
                .build());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "mock-refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));

        verify(authService).refresh("mock-refresh-token");
    }

    @Test
    void logoutShouldReturnNoContent() throws Exception {
        doNothing().when(authService).logout("mock-refresh-token");

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "mock-refresh-token"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(authService).logout(eq("mock-refresh-token"));
    }

    @Test
    void logoutShouldReturnBadRequestWhenRefreshTokenIsMissing() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
