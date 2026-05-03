package com.microservices.auth.util;

import com.microservices.auth.config.JwtConfig;
import com.microservices.auth.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long expiration;

    public JwtUtil(JwtConfig jwtConfig) {
        this.signingKey = Keys.hmacShaKeyFor(
                jwtConfig.secret().getBytes(StandardCharsets.UTF_8));
        this.expiration = jwtConfig.expiration();
    }

    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(signingKey)
                .compact();
    }
}
