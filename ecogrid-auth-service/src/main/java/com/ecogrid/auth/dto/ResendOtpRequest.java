package com.ecogrid.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class ResendOtpRequest {

    @NotBlank(message = "Email is required")
    private String email;

    private String purpose = "SIGNUP"; // default to SIGNUP if not specified

    public ResendOtpRequest() {}

    public ResendOtpRequest(String email, String purpose) {
        this.email = email;
        this.purpose = purpose;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
}
