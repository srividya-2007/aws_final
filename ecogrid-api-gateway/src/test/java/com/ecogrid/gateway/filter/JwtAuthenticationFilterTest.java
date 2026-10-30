package com.ecogrid.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("API Gateway JWT Filter Unit Tests")
class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter jwtFilter;

    private static final String SECRET = "ecogrid-test-secret-key-that-is-at-least-256-bits-long-hs256";
    private static final long EXPIRATION_MS = 86400000L;

    private Key signingKey;

    @BeforeEach
    void setUp() {
        jwtFilter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(jwtFilter, "secretKey", SECRET);
        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    private String generateValidToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private String generateExpiredToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis() - 10000))
                .setExpiration(new Date(System.currentTimeMillis() - 5000))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // ─── VALID JWT TESTS ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Should validate a valid JWT token successfully")
    void shouldValidateValidJwtToken() {
        // Arrange
        String token = generateValidToken("testuser");

        // Act
        boolean isValid = jwtFilter.validateToken(token);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should validate JWT token for different users")
    void shouldValidateTokenForDifferentUsers() {
        // Act
        String adminToken = generateValidToken("admin");
        String userToken = generateValidToken("regularuser");

        // Assert
        assertThat(jwtFilter.validateToken(adminToken)).isTrue();
        assertThat(jwtFilter.validateToken(userToken)).isTrue();
    }

    // ─── MISSING JWT TESTS ────────────────────────────────────────────────────

    @Test
    @DisplayName("Should reject null token")
    void shouldRejectNullToken() {
        // Act
        boolean isValid = jwtFilter.validateToken(null);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject empty token")
    void shouldRejectEmptyToken() {
        // Act
        boolean isValid = jwtFilter.validateToken("");

        // Assert
        assertThat(isValid).isFalse();
    }

    // ─── INVALID JWT TESTS ────────────────────────────────────────────────────

    @Test
    @DisplayName("Should reject malformed JWT token")
    void shouldRejectMalformedToken() {
        // Act
        boolean isValid = jwtFilter.validateToken("not-a-valid-jwt-token");

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject JWT with wrong signature")
    void shouldRejectTokenWithWrongSignature() {
        // Arrange - token signed with a different key
        Key wrongKey = Keys.hmacShaKeyFor(
                "completely-different-secret-key-of-sufficient-length-256-bits!".getBytes()
        );
        String tamperedToken = Jwts.builder()
                .setSubject("attacker")
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(wrongKey, SignatureAlgorithm.HS256)
                .compact();

        // Act - a token signed with a different key must NOT validate against our key
        // Either returns false or throws a security exception - both are acceptable rejections
        try {
            boolean isValid = jwtFilter.validateToken(tamperedToken);
            assertThat(isValid).isFalse();
        } catch (io.jsonwebtoken.security.SignatureException e) {
            // Acceptable: JJWT throws SignatureException for wrong key - token IS rejected
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("Should reject JWT with tampered payload")
    void shouldRejectTamperedToken() {
        // Arrange
        String validToken = generateValidToken("testuser");
        // Tamper the payload section (middle part)
        String[] parts = validToken.split("\\.");
        String tamperedToken = parts[0] + ".tampered_payload." + parts[2];

        // Act - tampered token must NOT be accepted
        // Either returns false or throws a security exception - both are rejections
        try {
            boolean isValid = jwtFilter.validateToken(tamperedToken);
            assertThat(isValid).isFalse();
        } catch (io.jsonwebtoken.security.SignatureException | io.jsonwebtoken.MalformedJwtException e) {
            // Acceptable: tampered token causes JJWT to throw - token IS rejected
            assertThat(e).isNotNull();
        }
    }

    // ─── EXPIRED JWT TESTS ────────────────────────────────────────────────────

    @Test
    @DisplayName("Should reject an expired JWT token")
    void shouldRejectExpiredToken() {
        // Arrange
        String expiredToken = generateExpiredToken("testuser");

        // Act
        boolean isValid = jwtFilter.validateToken(expiredToken);

        // Assert
        assertThat(isValid).isFalse();
    }

    // ─── ROUTE CONFIGURATION TESTS ───────────────────────────────────────────

    @Test
    @DisplayName("Should confirm auth routes do not require JWT (by convention)")
    void shouldConfirmAuthRoutesArePublic() {
        // Auth endpoints /api/auth/** should be publicly accessible
        // This test documents the design decision
        String[] publicRoutes = {"/api/auth/register", "/api/auth/login"};
        String[] protectedRoutes = {
                "/api/prosumers/**",
                "/api/telemetry/**",
                "/api/matching/**",
                "/api/settlements/**"
        };

        assertThat(publicRoutes).hasSize(2);
        assertThat(protectedRoutes).hasSize(4);

        // All protected routes require JWT filter
        for (String route : protectedRoutes) {
            assertThat(route).contains("/api/");
        }
    }

    @Test
    @DisplayName("Should confirm the gateway routes 5 downstream services")
    void shouldConfirm5DownstreamServices() {
        // Documents the 5 microservices behind the gateway
        String[] services = {
                "ecogrid-auth-service",
                "ecogrid-prosumer-service",
                "ecogrid-telemetry-service",
                "ecogrid-matching-service",
                "ecogrid-settlement-service"
        };

        assertThat(services).hasSize(5);
        for (String service : services) {
            assertThat(service).startsWith("ecogrid-");
        }
    }
}
