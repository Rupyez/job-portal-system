package com.jobportal.payload;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OAuth2ExchangeRequest {

    @NotBlank(message = "OAuth2 authorization code is required")
    private String code;
}