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
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered");
        }

        User user = new User(
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password()),
                Role.ROLE_USER
        );
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        return new AuthResponse(jwtUtil.generateToken(user));
    }

    @Transactional
    public void updateRole(Long userId, RoleUpdateRequest request, Role requesterRole) {
        if (requesterRole != Role.ROLE_ADMIN) {
            throw new UnauthorizedException("Insufficient permissions");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));

        user.setRole(request.role());
        userRepository.save(user);
    }
}
