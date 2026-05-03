package com.microservices.auth.service;

import com.microservices.auth.dto.AuthResponse;
import com.microservices.auth.dto.LoginRequest;
import com.microservices.auth.dto.RegisterRequest;
import com.microservices.auth.dto.RoleUpdateRequest;
import com.microservices.auth.entity.Role;
import com.microservices.auth.entity.User;
import com.microservices.auth.exception.BusinessException;
import com.microservices.auth.exception.ConflictException;
import com.microservices.auth.exception.UnauthorizedException;
import com.microservices.auth.repository.UserRepository;
import com.microservices.auth.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    // --- register ---

    @Test
    void register_shouldSaveUserWithEncodedPasswordAndDefaultRole() {
        RegisterRequest request = new RegisterRequest("John", "john@test.com", "password123");
        when(userRepository.existsByEmail("john@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("John");
        assertThat(saved.getEmail()).isEqualTo("john@test.com");
        assertThat(saved.getPassword()).isEqualTo("encoded-password");
        assertThat(saved.getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    void register_shouldThrowConflictException_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("John", "john@test.com", "password123");
        when(userRepository.existsByEmail("john@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Email already registered");

        verify(userRepository, never()).save(any());
    }

    // --- login ---

    @Test
    void login_shouldReturnAuthResponse_whenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("john@test.com", "password123");
        User user = new User("John", "john@test.com", "encoded-password", Role.ROLE_USER);
        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
    }

    @Test
    void login_shouldThrowUnauthorizedException_whenUserNotFound() {
        LoginRequest request = new LoginRequest("unknown@test.com", "password123");
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void login_shouldThrowUnauthorizedException_whenPasswordIsWrong() {
        LoginRequest request = new LoginRequest("john@test.com", "wrong-password");
        User user = new User("John", "john@test.com", "encoded-password", Role.ROLE_USER);
        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid credentials");

        verify(jwtUtil, never()).generateToken(any());
    }

    // --- updateRole ---

    @Test
    void updateRole_shouldUpdateRole_whenRequesterIsAdmin() {
        User user = new User("John", "john@test.com", "encoded-password", Role.ROLE_USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        authService.updateRole(1L, new RoleUpdateRequest(Role.ROLE_ADMIN), Role.ROLE_ADMIN);

        assertThat(user.getRole()).isEqualTo(Role.ROLE_ADMIN);
        verify(userRepository).save(user);
    }

    @Test
    void updateRole_shouldThrowUnauthorizedException_whenRequesterIsNotAdmin() {
        assertThatThrownBy(() ->
                authService.updateRole(1L, new RoleUpdateRequest(Role.ROLE_ADMIN), Role.ROLE_USER))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Insufficient permissions");

        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateRole_shouldThrowBusinessException_whenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.updateRole(99L, new RoleUpdateRequest(Role.ROLE_ADMIN), Role.ROLE_ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasMessage("User not found");
    }
}
