package com.ecogrid.auth.service;

import com.ecogrid.auth.dto.*;
import com.ecogrid.auth.model.*;
import com.ecogrid.auth.repository.OtpVerificationRepository;
import com.ecogrid.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OTP Service Unit Tests")
class OtpServiceTest {

    @Mock
    private OtpVerificationRepository otpRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private OtpService otpService;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private User testUser;
    private OtpVerification validSignupOtp;
    private OtpVerification validLoginOtp;

    @BeforeEach
    void setUp() {
        validRegisterRequest = new RegisterRequest("Test User", "testuser", "test@ecogrid.io", "password123", "USER");
        validLoginRequest = new LoginRequest("testuser", "password123");
        testUser = new User("testuser", "test@ecogrid.io", "encoded_password123", Role.USER);

        validSignupOtp = new OtpVerification("test@ecogrid.io", "123456", OtpPurpose.SIGNUP, LocalDateTime.now().plusMinutes(5));
        validSignupOtp.setUsername("testuser");
        validSignupOtp.setPasswordHash("encoded_password123");
        validSignupOtp.setRole(Role.USER);

        validLoginOtp = new OtpVerification("test@ecogrid.io", "654321", OtpPurpose.LOGIN, LocalDateTime.now().plusMinutes(5));
        validLoginOtp.setUsername("testuser");
    }

    // ─── OTP GENERATION ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should generate a valid 6-digit numeric OTP")
    void testGenerate6DigitOtp() {
        for (int i = 0; i < 50; i++) {
            String otp = otpService.generate6DigitOtp();
            assertThat(otp).isNotNull();
            assertThat(otp).matches("^\\d{6}$");
        }
    }

    // ─── SIGNUP WITH OTP ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Signup with OTP should store pending OTP and send email")
    void testInitiateSignup_Success() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@ecogrid.io")).thenReturn(false);
        when(otpRepository.findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(anyString(), any()))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_pass");

        AuthResponse res = otpService.initiateSignup(validRegisterRequest);

        assertThat(res.isOtpRequired()).isTrue();
        assertThat(res.getEmail()).isEqualTo("test@ecogrid.io");
        verify(otpRepository).save(any(OtpVerification.class));
        verify(emailService).sendOtpEmail(eq("test@ecogrid.io"), anyString(), eq("Account Registration"));
    }

    @Test
    @DisplayName("Signup with duplicate username should throw IllegalArgumentException")
    void testInitiateSignup_DuplicateUsername() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> otpService.initiateSignup(validRegisterRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");

        verify(otpRepository, never()).save(any());
    }

    @Test
    @DisplayName("Signup with duplicate email should throw IllegalArgumentException")
    void testInitiateSignup_DuplicateEmail() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@ecogrid.io")).thenReturn(true);

        assertThatThrownBy(() -> otpService.initiateSignup(validRegisterRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");
    }

    // ─── VERIFY SIGNUP OTP ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Correct signup OTP verification should create and save the user")
    void testVerifySignupOtp_Success() {
        when(otpRepository.findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc("test@ecogrid.io", OtpPurpose.SIGNUP))
                .thenReturn(Optional.of(validSignupOtp));
        when(userRepository.existsByUsername("testuser")).thenReturn(false);

        VerifyOtpRequest request = new VerifyOtpRequest("test@ecogrid.io", "123456");
        AuthResponse res = otpService.verifySignupOtp(request);

        assertThat(res.getMessage()).contains("Account verified and created successfully");
        assertThat(validSignupOtp.isUsed()).isTrue();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Wrong signup OTP should increment attempts and reject")
    void testVerifySignupOtp_WrongOtp() {
        when(otpRepository.findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc("test@ecogrid.io", OtpPurpose.SIGNUP))
                .thenReturn(Optional.of(validSignupOtp));

        VerifyOtpRequest request = new VerifyOtpRequest("test@ecogrid.io", "999999");

        assertThatThrownBy(() -> otpService.verifySignupOtp(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid OTP code");

        assertThat(validSignupOtp.getAttempts()).isEqualTo(1);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Expired OTP should be rejected")
    void testVerifySignupOtp_ExpiredOtp() {
        validSignupOtp.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // expired
        when(otpRepository.findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc("test@ecogrid.io", OtpPurpose.SIGNUP))
                .thenReturn(Optional.of(validSignupOtp));

        VerifyOtpRequest request = new VerifyOtpRequest("test@ecogrid.io", "123456");

        assertThatThrownBy(() -> otpService.verifySignupOtp(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("Exceeding 3 failed attempts should invalidate OTP and reject")
    void testVerifySignupOtp_MaxAttempts() {
        validSignupOtp.setAttempts(3);
        when(otpRepository.findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc("test@ecogrid.io", OtpPurpose.SIGNUP))
                .thenReturn(Optional.of(validSignupOtp));

        VerifyOtpRequest request = new VerifyOtpRequest("test@ecogrid.io", "123456");

        assertThatThrownBy(() -> otpService.verifySignupOtp(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Maximum verification attempts");
    }

    // ─── LOGIN WITH OTP ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Login with valid password should generate and send login OTP")
    void testInitiateLogin_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPassword())).thenReturn(true);
        when(otpRepository.findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(anyString(), eq(OtpPurpose.LOGIN)))
                .thenReturn(Optional.empty());

        AuthResponse res = otpService.initiateLogin(validLoginRequest);

        assertThat(res.isOtpRequired()).isTrue();
        verify(otpRepository).save(any(OtpVerification.class));
        verify(emailService).sendOtpEmail(eq("test@ecogrid.io"), anyString(), eq("Account Login"));
    }

    @Test
    @DisplayName("Login with invalid password should throw BadCredentialsException")
    void testInitiateLogin_WrongPassword() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpass", testUser.getPassword())).thenReturn(false);

        LoginRequest wrongReq = new LoginRequest("testuser", "wrongpass");

        assertThatThrownBy(() -> otpService.initiateLogin(wrongReq))
                .isInstanceOf(BadCredentialsException.class);

        verify(otpRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login for non-existent user should throw BadCredentialsException")
    void testInitiateLogin_UserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());

        LoginRequest unknownReq = new LoginRequest("unknown", "pass");

        assertThatThrownBy(() -> otpService.initiateLogin(unknownReq))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ─── VERIFY LOGIN OTP ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Correct login OTP verification should return Access Token and Refresh Token")
    void testVerifyLoginOtp_Success() {
        when(otpRepository.findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc("test@ecogrid.io", OtpPurpose.LOGIN))
                .thenReturn(Optional.of(validLoginOtp));
        when(userRepository.findByEmail("test@ecogrid.io")).thenReturn(Optional.of(testUser));

        UserDetails mockUserDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(mockUserDetails);
        when(jwtService.generateToken(mockUserDetails)).thenReturn("mock.access.token");
        when(jwtService.generateRefreshToken(mockUserDetails)).thenReturn("mock.refresh.token");

        VerifyOtpRequest request = new VerifyOtpRequest("test@ecogrid.io", "654321");
        AuthResponse res = otpService.verifyLoginOtp(request);

        assertThat(res.getToken()).isEqualTo("mock.access.token");
        assertThat(res.getRefreshToken()).isEqualTo("mock.refresh.token");
        assertThat(res.getUsername()).isEqualTo("testuser");
        assertThat(res.getRole()).isEqualTo("USER");
        assertThat(validLoginOtp.isUsed()).isTrue();
    }

    // ─── RESEND OTP & RATE LIMITING ───────────────────────────────────────────

    @Test
    @DisplayName("Resend OTP within 30s cooldown should throw rate limit exception")
    void testResendOtp_RateLimit() {
        validSignupOtp.setCreatedAt(LocalDateTime.now().minusSeconds(10)); // only 10s ago
        when(otpRepository.findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc("test@ecogrid.io", OtpPurpose.SIGNUP))
                .thenReturn(Optional.of(validSignupOtp));

        ResendOtpRequest req = new ResendOtpRequest("test@ecogrid.io", "SIGNUP");

        assertThatThrownBy(() -> otpService.resendOtp(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Please wait");

        verify(emailService, never()).sendOtpEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Resend OTP after 30s should succeed and dispatch a fresh code")
    void testResendOtp_Success() {
        validSignupOtp.setCreatedAt(LocalDateTime.now().minusSeconds(35)); // 35s ago
        when(otpRepository.findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc("test@ecogrid.io", OtpPurpose.SIGNUP))
                .thenReturn(Optional.of(validSignupOtp));

        ResendOtpRequest req = new ResendOtpRequest("test@ecogrid.io", "SIGNUP");
        AuthResponse res = otpService.resendOtp(req);

        assertThat(res.getMessage()).contains("new 6-digit verification code has been sent");
        verify(emailService).sendOtpEmail(eq("test@ecogrid.io"), anyString(), eq("Account Registration"));
    }
}
