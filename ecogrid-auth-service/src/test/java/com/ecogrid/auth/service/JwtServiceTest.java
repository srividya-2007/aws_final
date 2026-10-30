package com.ecogrid.auth.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Service Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;

    private UserDetails testUserDetails;

    private static final String SECRET = "ecogrid-test-secret-key-that-is-at-least-256-bits-long-hs256";
    private static final long EXPIRATION_MS = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION_MS);

        testUserDetails = new User(
                "testuser",
                "encodedPassword",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    // ─── TOKEN GENERATION TESTS ───────────────────────────────────────────────

    @Test
    @DisplayName("Should generate a non-null JWT token")
    void shouldGenerateNonNullToken() {
        // Act
        String token = jwtService.generateToken(testUserDetails);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Should generate token with correct username as subject")
    void shouldGenerateTokenWithCorrectUsername() {
        // Act
        String token = jwtService.generateToken(testUserDetails);
        String extractedUsername = jwtService.extractUsername(token);

        // Assert
        assertThat(extractedUsername).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void shouldGenerateDifferentTokensForDifferentUsers() {
        // Arrange
        UserDetails anotherUser = new User("anotheruser", "pass",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        // Act
        String token1 = jwtService.generateToken(testUserDetails);
        String token2 = jwtService.generateToken(anotherUser);

        // Assert
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Should generate token that is a valid JWT structure (3 parts)")
    void shouldGenerateWellStructuredJwt() {
        // Act
        String token = jwtService.generateToken(testUserDetails);
        String[] parts = token.split("\\.");

        // Assert - JWT has header.payload.signature
        assertThat(parts).hasSize(3);
    }

    // ─── TOKEN VALIDATION TESTS ───────────────────────────────────────────────

    @Test
    @DisplayName("Should validate a valid token successfully")
    void shouldValidateValidToken() {
        // Act
        String token = jwtService.generateToken(testUserDetails);
        boolean isValid = jwtService.isTokenValid(token, testUserDetails);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject token for wrong user")
    void shouldRejectTokenForWrongUser() {
        // Arrange
        UserDetails differentUser = new User("differentuser", "pass",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        // Act
        String token = jwtService.generateToken(testUserDetails);
        boolean isValid = jwtService.isTokenValid(token, differentUser);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should correctly identify a non-expired token")
    void shouldIdentifyNonExpiredToken() {
        // Act
        String token = jwtService.generateToken(testUserDetails);
        boolean isExpired = jwtService.isTokenExpired(token);

        // Assert
        assertThat(isExpired).isFalse();
    }

    // ─── EXPIRED TOKEN TESTS ─────────────────────────────────────────────────

    @Test
    @DisplayName("Should reject an expired token")
    void shouldRejectExpiredToken() {
        // Arrange - generate token that expires in 1 ms
        String expiredToken = jwtService.generateTokenWithExpiration(testUserDetails, 1L);

        // Wait for it to expire
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act & Assert - extracting username from expired token throws ExpiredJwtException
        assertThatThrownBy(() -> jwtService.extractUsername(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Should throw exception for malformed token")
    void shouldThrowExceptionForMalformedToken() {
        // Act & Assert
        assertThatThrownBy(() -> jwtService.extractUsername("not.a.valid.jwt.token"))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should throw exception for tampered token signature")
    void shouldThrowExceptionForTamperedToken() {
        // Arrange
        String validToken = jwtService.generateToken(testUserDetails);
        String tamperedToken = validToken.substring(0, validToken.lastIndexOf('.') + 1) + "tamperedSignature";

        // Act & Assert
        assertThatThrownBy(() -> jwtService.extractUsername(tamperedToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should correctly report JWT expiration duration")
    void shouldReportCorrectExpirationDuration() {
        // Act
        long expiration = jwtService.getJwtExpiration();

        // Assert
        assertThat(expiration).isEqualTo(EXPIRATION_MS);
    }

    // ─── BCrypt PASSWORD TESTS ────────────────────────────────────────────────

    @Test
    @DisplayName("Should verify BCrypt encoded password matches raw password")
    void shouldVerifyBCryptEncodedPassword() {
        // Arrange
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String rawPassword = "mySecurePassword123";

        // Act
        String encodedPassword = encoder.encode(rawPassword);
        boolean matches = encoder.matches(rawPassword, encodedPassword);

        // Assert
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should produce different BCrypt hashes for same password")
    void shouldProduceDifferentHashesForSamePassword() {
        // Arrange
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String rawPassword = "myPassword";

        // Act
        String hash1 = encoder.encode(rawPassword);
        String hash2 = encoder.encode(rawPassword);

        // Assert - BCrypt uses random salt, so hashes differ but both match
        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(encoder.matches(rawPassword, hash1)).isTrue();
        assertThat(encoder.matches(rawPassword, hash2)).isTrue();
    }

    @Test
    @DisplayName("Should not match wrong password with BCrypt hash")
    void shouldNotMatchWrongPasswordWithBCryptHash() {
        // Arrange
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String encodedPassword = encoder.encode("correctPassword");

        // Act
        boolean matches = encoder.matches("wrongPassword", encodedPassword);

        // Assert
        assertThat(matches).isFalse();
    }
}
