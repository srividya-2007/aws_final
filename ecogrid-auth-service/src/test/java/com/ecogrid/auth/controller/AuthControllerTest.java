package com.ecogrid.auth.controller;

import com.ecogrid.auth.dto.AuthResponse;
import com.ecogrid.auth.dto.LoginRequest;
import com.ecogrid.auth.dto.RegisterRequest;
import com.ecogrid.auth.service.AuthService;
import com.ecogrid.auth.service.JwtService;
import com.ecogrid.auth.service.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer unit tests using MockMvc.
 * Security filters are disabled (addFilters=false) because security
 * is tested separately at the gateway / integration test level.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)   // Disable security filters for controller unit tests
@TestPropertySource(properties = {
    "jwt.secret=ecogrid-test-secret-key-that-is-at-least-256-bits-long-hs256",
    "jwt.expiration=86400000",
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false"
})
@DisplayName("Auth Controller MockMvc Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    // Required by SecurityConfig even when filters are disabled
    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    // ─── REGISTER ENDPOINT TESTS ──────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/register - Should return 201 CREATED on success")
    void shouldReturn201OnSuccessfulRegistration() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest("newuser", "new@example.com", "password123");
        AuthResponse response = new AuthResponse("jwt-token", "newuser", "USER");
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 400 for blank username (validation)")
    void shouldReturn400ForBlankUsername() throws Exception {
        // Arrange - blank username violates @NotBlank constraint
        RegisterRequest badRequest = new RegisterRequest("", "valid@email.com", "password123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 409 CONFLICT for duplicate username")
    void shouldReturn409ForDuplicateUsername() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest("existinguser", "new@email.com", "password123");
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Username already exists: existinguser"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // ─── LOGIN ENDPOINT TESTS ─────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/login - Should return 200 OK with JWT token on success")
    void shouldReturn200OnSuccessfulLogin() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("johndoe", "password123");
        AuthResponse response = new AuthResponse("valid-token", "johndoe", "USER");
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("valid-token"))
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 401 for invalid credentials")
    void shouldReturn401ForInvalidCredentials() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("johndoe", "wrongPass");
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
