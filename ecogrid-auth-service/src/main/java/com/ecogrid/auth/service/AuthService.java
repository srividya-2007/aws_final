package com.ecogrid.auth.service;

import com.ecogrid.auth.dto.*;
import com.ecogrid.auth.model.Role;
import com.ecogrid.auth.model.User;
import com.ecogrid.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired(required = false)
    private OtpService otpService;

    // ─── Direct Register & Login (Maintained for backward compatibility) ─────

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        // Resolve role — use provided value or default to USER
        Role assignedRole;
        try {
            assignedRole = (request.getRole() != null && !request.getRole().isBlank())
                    ? Role.valueOf(request.getRole().toUpperCase())
                    : Role.USER;
        } catch (IllegalArgumentException e) {
            assignedRole = Role.USER;
        }

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                assignedRole
        );

        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(token, user.getUsername(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        if (otpService != null) {
            return otpService.initiateLogin(request);
        }
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid username or password");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtService.generateToken(userDetails);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return new AuthResponse(token, user.getUsername(), user.getRole().name());
    }

    // ─── OTP-Based Flows ─────────────────────────────────────────────────────

    public AuthResponse signupWithOtp(RegisterRequest request) {
        if (otpService == null) {
            throw new IllegalStateException("OTP Service is not configured");
        }
        return otpService.initiateSignup(request);
    }

    public AuthResponse verifySignupOtp(VerifyOtpRequest request) {
        if (otpService == null) {
            throw new IllegalStateException("OTP Service is not configured");
        }
        return otpService.verifySignupOtp(request);
    }

    public AuthResponse loginWithOtp(LoginRequest request) {
        if (otpService == null) {
            throw new IllegalStateException("OTP Service is not configured");
        }
        return otpService.initiateLogin(request);
    }

    public AuthResponse verifyLoginOtp(VerifyOtpRequest request) {
        if (otpService == null) {
            throw new IllegalStateException("OTP Service is not configured");
        }
        return otpService.verifyLoginOtp(request);
    }

    public AuthResponse resendOtp(ResendOtpRequest request) {
        if (otpService == null) {
            throw new IllegalStateException("OTP Service is not configured");
        }
        return otpService.resendOtp(request);
    }
}
