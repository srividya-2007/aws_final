package com.ecogrid.auth.service;

import com.ecogrid.auth.dto.*;
import com.ecogrid.auth.model.OtpPurpose;
import com.ecogrid.auth.model.OtpVerification;
import com.ecogrid.auth.model.Role;
import com.ecogrid.auth.model.User;
import com.ecogrid.auth.repository.OtpVerificationRepository;
import com.ecogrid.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final int OTP_VALIDITY_MINUTES = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 30;
    private static final int MAX_ATTEMPTS = 3;

    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private OtpVerificationRepository otpRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * Generate 6-digit numeric OTP
     */
    public String generate6DigitOtp() {
        int code = secureRandom.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    /**
     * Step 1 of Signup: Validate details, generate OTP, store pending registration, send email.
     */
    @Transactional
    public AuthResponse initiateSignup(RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters");
        }
        if (request.getEmail() == null || !request.getEmail().contains("@")) {
            throw new IllegalArgumentException("Valid email is required");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }

        // Rate limit check: ensure 30s has passed since last OTP request
        Optional<OtpVerification> latestOpt = otpRepository.findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                email, OtpPurpose.SIGNUP);
        if (latestOpt.isPresent()) {
            LocalDateTime created = latestOpt.get().getCreatedAt();
            if (created.isAfter(LocalDateTime.now().minusSeconds(RESEND_COOLDOWN_SECONDS))) {
                throw new IllegalArgumentException("Please wait " + RESEND_COOLDOWN_SECONDS + " seconds before requesting a new code.");
            }
            // Invalidate prior unused OTPs
            invalidateUnusedOtps(email, OtpPurpose.SIGNUP);
        }

        // Resolve role
        Role assignedRole = Role.USER;
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                assignedRole = Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                assignedRole = Role.USER;
            }
        }

        String otpCode = generate6DigitOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES);

        OtpVerification otp = new OtpVerification(email, otpCode, OtpPurpose.SIGNUP, expiresAt);
        otp.setUsername(username);
        otp.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        otp.setRole(assignedRole);
        otp.setFullName(request.getFullName() != null ? request.getFullName().trim() : username);

        otpRepository.save(otp);

        emailService.sendOtpEmail(email, otpCode, "Account Registration");

        return AuthResponse.otpRequired(email, username, "Verification code sent to " + email);
    }

    /**
     * Step 2 of Signup: Verify OTP, check expiry/attempts, create User account.
     */
    @Transactional
    public AuthResponse verifySignupOtp(VerifyOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String submittedCode = request.getOtpCode() != null ? request.getOtpCode().trim() : "";

        OtpVerification otp = otpRepository.findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                email, OtpPurpose.SIGNUP)
                .orElseThrow(() -> new IllegalArgumentException("No pending verification found for " + email + ". Please sign up again."));

        validateOtpAttempt(otp, submittedCode);

        // Mark OTP as used
        otp.setUsed(true);
        otpRepository.save(otp);

        // Double check user doesn't exist now
        if (userRepository.existsByUsername(otp.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + otp.getUsername());
        }

        // Create user entity
        User newUser = new User(
                otp.getUsername(),
                otp.getEmail(),
                otp.getPasswordHash(),
                otp.getRole() != null ? otp.getRole() : Role.USER
        );
        userRepository.save(newUser);

        log.info("User created successfully via OTP verification: {}", newUser.getUsername());

        AuthResponse response = new AuthResponse();
        response.setUsername(newUser.getUsername());
        response.setRole(newUser.getRole().name());
        response.setMessage("Account verified and created successfully. You can now login.");
        return response;
    }

    /**
     * Step 1 of Login: Validate credentials, generate OTP, send to registered email.
     */
    @Transactional
    public AuthResponse initiateLogin(LoginRequest request) {
        String loginIdentifier = request.getUsername().trim();
        String password = request.getPassword();

        // Support login by either username or email
        User user = userRepository.findByUsername(loginIdentifier)
                .or(() -> userRepository.findByEmail(loginIdentifier))
                .orElseThrow(() -> new BadCredentialsException("Invalid username/email or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid username/email or password");
        }

        String email = user.getEmail().toLowerCase();

        // Check 30-second cooldown
        Optional<OtpVerification> latestOpt = otpRepository.findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                email, OtpPurpose.LOGIN);
        if (latestOpt.isPresent()) {
            LocalDateTime created = latestOpt.get().getCreatedAt();
            if (created.isAfter(LocalDateTime.now().minusSeconds(RESEND_COOLDOWN_SECONDS))) {
                throw new IllegalArgumentException("Please wait " + RESEND_COOLDOWN_SECONDS + " seconds before requesting a new code.");
            }
            invalidateUnusedOtps(email, OtpPurpose.LOGIN);
        }

        String otpCode = generate6DigitOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES);

        OtpVerification otp = new OtpVerification(email, otpCode, OtpPurpose.LOGIN, expiresAt);
        otp.setUsername(user.getUsername());
        otpRepository.save(otp);

        emailService.sendOtpEmail(email, otpCode, "Account Login");

        return AuthResponse.otpRequired(email, user.getUsername(), "Verification code sent to " + maskEmail(email));
    }

    /**
     * Step 2 of Login: Verify OTP, generate JWT Access and Refresh Tokens.
     */
    @Transactional
    public AuthResponse verifyLoginOtp(VerifyOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String submittedCode = request.getOtpCode() != null ? request.getOtpCode().trim() : "";

        OtpVerification otp = otpRepository.findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                email, OtpPurpose.LOGIN)
                .orElseThrow(() -> new IllegalArgumentException("No active login OTP found for " + email + ". Please sign in again."));

        validateOtpAttempt(otp, submittedCode);

        // Mark OTP as used
        otp.setUsed(true);
        otpRepository.save(otp);

        User user = userRepository.findByEmail(email)
                .or(() -> userRepository.findByUsername(otp.getUsername()))
                .orElseThrow(() -> new IllegalArgumentException("User record not found for: " + email));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        log.info("User logged in successfully via OTP verification: {}", user.getUsername());

        AuthResponse response = new AuthResponse(accessToken, refreshToken, user.getUsername(), user.getRole().name());
        response.setEmail(user.getEmail());
        response.setMessage("Login verified successfully.");
        return response;
    }

    /**
     * Resend OTP for either SIGNUP or LOGIN.
     */
    @Transactional
    public AuthResponse resendOtp(ResendOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        OtpPurpose purpose;
        try {
            purpose = OtpPurpose.valueOf(request.getPurpose().toUpperCase());
        } catch (Exception e) {
            purpose = OtpPurpose.SIGNUP;
        }

        Optional<OtpVerification> latestOpt = otpRepository.findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                email, purpose);

        if (latestOpt.isPresent()) {
            LocalDateTime created = latestOpt.get().getCreatedAt();
            if (created.isAfter(LocalDateTime.now().minusSeconds(RESEND_COOLDOWN_SECONDS))) {
                long remaining = RESEND_COOLDOWN_SECONDS - java.time.Duration.between(created, LocalDateTime.now()).getSeconds();
                throw new IllegalArgumentException("Please wait " + Math.max(1, remaining) + " seconds before requesting a new OTP.");
            }
        }

        String newOtpCode = generate6DigitOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES);

        OtpVerification newOtp;
        if (purpose == OtpPurpose.SIGNUP) {
            // Need to retain pending user details
            OtpVerification prior = latestOpt.orElseThrow(() ->
                    new IllegalArgumentException("No pending signup found for " + email + ". Please restart registration."));
            invalidateUnusedOtps(email, purpose);

            newOtp = new OtpVerification(email, newOtpCode, purpose, expiresAt);
            newOtp.setUsername(prior.getUsername());
            newOtp.setPasswordHash(prior.getPasswordHash());
            newOtp.setRole(prior.getRole());
            newOtp.setFullName(prior.getFullName());
        } else {
            // LOGIN
            User user = userRepository.findByEmail(email).orElseThrow(() ->
                    new IllegalArgumentException("No account registered with email: " + email));
            invalidateUnusedOtps(email, purpose);

            newOtp = new OtpVerification(email, newOtpCode, purpose, expiresAt);
            newOtp.setUsername(user.getUsername());
        }

        otpRepository.save(newOtp);

        emailService.sendOtpEmail(email, newOtpCode, purpose == OtpPurpose.SIGNUP ? "Account Registration" : "Account Login");

        AuthResponse response = new AuthResponse();
        response.setEmail(email);
        response.setMessage("A new 6-digit verification code has been sent to " + maskEmail(email));
        return response;
    }

    private void validateOtpAttempt(OtpVerification otp, String submittedCode) {
        if (otp.isExpired()) {
            otp.setUsed(true);
            otpRepository.save(otp);
            throw new IllegalArgumentException("OTP code has expired. Please request a new code.");
        }

        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            otp.setUsed(true);
            otpRepository.save(otp);
            throw new IllegalArgumentException("Maximum verification attempts (" + MAX_ATTEMPTS + ") exceeded. Please request a new code.");
        }

        if (!otp.getOtpCode().equals(submittedCode)) {
            otp.setAttempts(otp.getAttempts() + 1);
            otpRepository.save(otp);
            int remaining = MAX_ATTEMPTS - otp.getAttempts();
            if (remaining <= 0) {
                otp.setUsed(true);
                otpRepository.save(otp);
                throw new IllegalArgumentException("Maximum verification attempts exceeded. Please request a new code.");
            }
            throw new IllegalArgumentException("Invalid OTP code. " + remaining + " attempt(s) remaining.");
        }
    }

    private void invalidateUnusedOtps(String email, OtpPurpose purpose) {
        List<OtpVerification> list = otpRepository.findAllByEmailAndPurposeAndUsedFalse(email, purpose);
        for (OtpVerification item : list) {
            item.setUsed(true);
        }
        otpRepository.saveAll(list);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int at = email.indexOf("@");
        if (at <= 2) return email;
        return email.substring(0, 2) + "***" + email.substring(at - 1);
    }
}
