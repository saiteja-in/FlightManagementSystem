package com.saiteja.apigateway.dto.request;

import jakarta.validation.constraints.NotBlank;

public class GoogleTokenRequest {
    @NotBlank
    private String idToken;

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}

