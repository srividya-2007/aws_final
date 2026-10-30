package com.ecogrid.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyOtpRequest {

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "OTP code is required")
    private String otpCode;

    private String username;

    public VerifyOtpRequest() {}

    public VerifyOtpRequest(String email, String otpCode) {
        this.email = email;
        this.otpCode = otpCode;
    }

    public VerifyOtpRequest(String email, String otpCode, String username) {
        this.email = email;
        this.otpCode = otpCode;
        this.username = username;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
