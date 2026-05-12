package com.smartworks.smartworks_api.dto;

public class AuthResponse {
    public String token;
    public String username;
    public String role;

    public AuthResponse(String token, String username, String role) {
        this.token = token;
        this.username = username;
        this.role = role;
    }
}