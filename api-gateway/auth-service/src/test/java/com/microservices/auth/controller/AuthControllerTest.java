package com.microservices.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.auth.dto.AuthResponse;
import com.microservices.auth.dto.LoginRequest;
import com.microservices.auth.dto.RegisterRequest;
import com.microservices.auth.dto.RoleUpdateRequest;
import com.microservices.auth.entity.Role;
import com.microservices.auth.exception.ConflictException;
import com.microservices.auth.exception.GlobalExceptionHandler;
import com.microservices.auth.exception.UnauthorizedException;
import com.microservices.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    // --- POST /api/auth/register ---

    @Test
    void register_shouldReturn201_whenRequestIsValid() throws Exception {
        RegisterRequest request = new RegisterRequest("John", "john@test.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void register_shouldReturn400_whenNameIsBlank() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"john@test.com","password":"password123"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").value("Name is required"));
    }

    @Test
    void register_shouldReturn400_whenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"John","email":"not-an-email","password":"password123"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.email").value("Invalid email format"));
    }

    @Test
    void register_shouldReturn400_whenPasswordIsTooShort() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"John","email":"john@test.com","password":"short"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.password").value("Password must be at least 8 characters"));
    }

    @Test
    void register_shouldReturn409_whenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest("John", "john@test.com", "password123");
        doThrow(new ConflictException("Email already registered"))
                .when(authService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    // --- POST /api/auth/login ---

    @Test
    void login_shouldReturn200WithToken_whenCredentialsAreValid() throws Exception {
        LoginRequest request = new LoginRequest("john@test.com", "password123");
        when(authService.login(any(LoginRequest.class))).thenReturn(new AuthResponse("jwt-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_shouldReturn401_whenCredentialsAreInvalid() throws Exception {
        LoginRequest request = new LoginRequest("john@test.com", "wrong-password");
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void login_shouldReturn400_whenEmailIsBlank() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"","password":"password123"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.email").exists());
    }

    // --- PATCH /api/auth/users/{id}/role ---

    @Test
    void updateRole_shouldReturn204_whenRequestIsValid() throws Exception {
        RoleUpdateRequest request = new RoleUpdateRequest(Role.ROLE_ADMIN);

        mockMvc.perform(patch("/api/auth/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Role", "ROLE_ADMIN")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(authService).updateRole(eq(1L), any(RoleUpdateRequest.class), eq(Role.ROLE_ADMIN));
    }

    @Test
    void updateRole_shouldReturn401_whenRequesterIsNotAdmin() throws Exception {
        doThrow(new UnauthorizedException("Insufficient permissions"))
                .when(authService).updateRole(eq(1L), any(RoleUpdateRequest.class), eq(Role.ROLE_USER));

        mockMvc.perform(patch("/api/auth/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Role", "ROLE_USER")
                        .content("""
                                {"role":"ROLE_ADMIN"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Insufficient permissions"));
    }

    @Test
    void updateRole_shouldReturn400_whenRoleIsNull() throws Exception {
        mockMvc.perform(patch("/api/auth/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Role", "ROLE_ADMIN")
                        .content("""
                                {"role":null}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.role").value("Role is required"));
    }

    @Test
    void updateRole_shouldReturn400_whenRoleIsInvalidEnumValue() throws Exception {
        mockMvc.perform(patch("/api/auth/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Role", "ROLE_ADMIN")
                        .content("""
                                {"role":"INVALID_ROLE"}"""))
                .andExpect(status().isBadRequest());
    }
}
