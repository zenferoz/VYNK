package com.vynk.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VerifyEmailRequest {

    @NotBlank(message = "Verification token is required")
    @Size(max = 255, message = "Verification token is invalid")
    private String token;

    public VerifyEmailRequest() {
    }

    public VerifyEmailRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
