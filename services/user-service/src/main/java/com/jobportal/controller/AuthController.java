package com.jobportal.controller;


import com.jobportal.payload.AuthResponse;
import com.jobportal.payload.LoginRequest;
import com.jobportal.payload.RefreshTokenRequest;
import com.jobportal.payload.SignupRequest;
import com.jobportal.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor

public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody @Valid SignupRequest request) throws Exception{
       return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request){
        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refrsh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request){
        //pass the refresh token to the service layer
        //the service handles validation, rotation and persistence
        AuthResponse response = authService.refresh(request.getRefreshToken());

        return ResponseEntity.ok(response);
    }


    /**
     * Logs the user out by revoking their refresh token.
     *
     * The client should discard its local authentication tokens
     * after receiving a successful response.
     */

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request){

        //Ask the service to revoke the supplied refresh token
        authService.logout(request.getRefreshToken());

        return ResponseEntity. noContent().build();
    }

}
