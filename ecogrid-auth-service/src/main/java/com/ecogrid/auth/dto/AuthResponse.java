package com.ecogrid.auth.dto;

public class AuthResponse {

    private String token;
    private String refreshToken;
    private String username;
    private String role;
    private String message;
    private String email;
    private boolean otpRequired;

    public AuthResponse() {}

    public AuthResponse(String token, String username, String role) {
        this.token = token;
        this.username = username;
        this.role = role;
    }

    public AuthResponse(String token, String refreshToken, String username, String role) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.username = username;
        this.role = role;
    }

    public AuthResponse(String message) {
        this.message = message;
    }

    public static AuthResponse otpRequired(String email, String username, String message) {
        AuthResponse res = new AuthResponse();
        res.setEmail(email);
        res.setUsername(username);
        res.setMessage(message);
        res.setOtpRequired(true);
        return res;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isOtpRequired() { return otpRequired; }
    public void setOtpRequired(boolean otpRequired) { this.otpRequired = otpRequired; }
}
