package com.ecogrid.auth.service;

import com.ecogrid.auth.dto.AuthResponse;
import com.ecogrid.auth.dto.LoginRequest;
import com.ecogrid.auth.dto.RegisterRequest;
import com.ecogrid.auth.model.Role;
import com.ecogrid.auth.model.User;
import com.ecogrid.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private User savedUser;
    private UserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        validRegisterRequest = new RegisterRequest("johndoe", "john@example.com", "securePass123");
        validLoginRequest = new LoginRequest("johndoe", "securePass123");

        savedUser = new User("johndoe", "john@example.com", "encodedPassword", Role.USER);
        savedUser.setId(1L);

        mockUserDetails = new org.springframework.security.core.userdetails.User(
                "johndoe",
                "encodedPassword",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    // ─── REGISTRATION TESTS ──────────────────────────────────────────────────

    @Test
    @DisplayName("Should successfully register a new user")
    void shouldRegisterNewUserSuccessfully() {
        // Arrange
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("securePass123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(mockUserDetails);
        when(jwtService.generateToken(mockUserDetails)).thenReturn("mocked-jwt-token");

        // Act
        AuthResponse response = authService.register(validRegisterRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mocked-jwt-token");
        assertThat(response.getUsername()).isEqualTo("johndoe");
        assertThat(response.getRole()).isEqualTo("USER");

        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("securePass123");
    }

    @Test
    @DisplayName("Should reject registration when username already exists")
    void shouldRejectRegistrationWhenUsernameAlreadyExists() {
        // Arrange
        when(userRepository.existsByUsername("johndoe")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject registration when email already registered")
    void shouldRejectRegistrationWhenEmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should encode password with BCrypt before saving")
    void shouldEncodePasswordBeforeSaving() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("securePass123")).thenReturn("$2a$10$encodedHash");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(mockUserDetails);
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("token");

        // Act
        authService.register(validRegisterRequest);

        // Assert - verify BCrypt encoding was called
        verify(passwordEncoder).encode("securePass123");
        verify(userRepository).save(argThat(user ->
                !user.getPassword().equals("securePass123") // raw password not stored
        ));
    }

    // ─── LOGIN TESTS ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should successfully login with correct credentials")
    void shouldLoginSuccessfullyWithCorrectCredentials() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null); // returns auth token on success
        when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(mockUserDetails);
        when(jwtService.generateToken(mockUserDetails)).thenReturn("valid-jwt-token");
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(savedUser));

        // Act
        AuthResponse response = authService.login(validLoginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("valid-jwt-token");
        assertThat(response.getUsername()).isEqualTo("johndoe");
    }

    @Test
    @DisplayName("Should throw exception for invalid password")
    void shouldThrowExceptionForInvalidPassword() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest badRequest = new LoginRequest("johndoe", "wrongPassword");

        // Act & Assert
        assertThatThrownBy(() -> authService.login(badRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    @DisplayName("Should generate JWT token upon successful login")
    void shouldGenerateJwtTokenOnSuccessfulLogin() {
        // Arrange
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(mockUserDetails);
        when(jwtService.generateToken(mockUserDetails)).thenReturn("generated-jwt-12345");
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(savedUser));

        // Act
        AuthResponse response = authService.login(validLoginRequest);

        // Assert
        assertThat(response.getToken()).isNotNull();
        assertThat(response.getToken()).isEqualTo("generated-jwt-12345");
        verify(jwtService).generateToken(mockUserDetails);
    }
}
