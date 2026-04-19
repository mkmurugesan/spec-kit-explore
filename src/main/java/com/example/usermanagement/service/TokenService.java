package com.example.usermanagement.service;

import com.example.usermanagement.config.JwtProperties;
import com.example.usermanagement.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class TokenService {

    private final JwtProperties jwtProperties;

    public TokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getEmail())
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtProperties.getAccessTokenExpiry().toMillis()))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getEmail())
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtProperties.getRefreshTokenExpiry().toMillis()))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = getClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}


