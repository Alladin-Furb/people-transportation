package com.microservices.auth.util;

import com.microservices.auth.config.JwtConfig;
import com.microservices.auth.entity.Role;
import com.microservices.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET = "my-super-secret-key-that-is-at-least-32-chars-long!";
    private static final long EXPIRATION = 3600000L;

    private JwtUtil jwtUtil;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        JwtConfig config = new JwtConfig(SECRET, EXPIRATION);
        jwtUtil = new JwtUtil(config);
        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void generateToken_shouldContainUserIdAsSubject() throws Exception {
        User user = createUserWithId(1L, "John", "john@test.com", "password", Role.ROLE_USER);

        String token = jwtUtil.generateToken(user);
        Claims claims = parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("1");
    }

    @Test
    void generateToken_shouldContainRoleClaim() throws Exception {
        User user = createUserWithId(1L, "John", "john@test.com", "password", Role.ROLE_ADMIN);

        String token = jwtUtil.generateToken(user);
        Claims claims = parseToken(token);

        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void generateToken_shouldSetIssuedAtAndExpiration() throws Exception {
        User user = createUserWithId(1L, "John", "john@test.com", "password", Role.ROLE_USER);

        String token = jwtUtil.generateToken(user);
        Claims claims = parseToken(token);

        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                .isEqualTo(EXPIRATION);
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private User createUserWithId(Long id, String name, String email, String password, Role role) throws Exception {
        User user = new User(name, email, password, role);
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
        return user;
    }
}
