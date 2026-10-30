package com.ecogrid.auth.controller;

import com.ecogrid.auth.dto.*;
import com.ecogrid.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/auth", "/auth"})
public class AuthController {

    @Autowired
    private AuthService authService;

    // ─── SIGNUP (OTP STEP 1 & 2) ───────────────────────────

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.signupWithOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-signup-otp")
    public ResponseEntity<AuthResponse> verifySignupOtp(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifySignupOtp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Direct registration (legacy & testing compatibility)
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── LOGIN (OTP STEP 1 & 2) ────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-login-otp")
    public ResponseEntity<AuthResponse> verifyLoginOtp(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifyLoginOtp(request);
        return ResponseEntity.ok(response);
    }

    // ─── RESEND OTP ────────────────────────────────────────

    @PostMapping("/resend-otp")
    public ResponseEntity<AuthResponse> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        AuthResponse response = authService.resendOtp(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth Service is UP");
    }
}
