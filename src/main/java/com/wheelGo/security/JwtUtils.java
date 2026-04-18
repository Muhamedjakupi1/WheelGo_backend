package com.wheelGo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    public String generateToken(CustomUserPrincipal p) {
        return Jwts.builder()
                .setSubject(p.getEmail())
                .claim("userId", p.getUserId() != null ? p.getUserId().toString() : null)
                .claim("role", p.getRole())
                .claim("tenantId", p.getTenantId() != null ? p.getTenantId().toString() : null)
                .claim("tenantSlug", p.getTenantSlug())
                .claim("isImpersonating", p.isImpersonating())
                .claim("originalRole", p.getOriginalRole())
                .claim("originalUserId", p.getOriginalUserId() != null ? p.getOriginalUserId().toString() : null)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateImpersonationToken(CustomUserPrincipal target,
                                             UUID originalUserId,
                                             String originalRole) {
        return Jwts.builder()
                .setSubject(target.getEmail())
                .claim("userId", target.getUserId() != null ? target.getUserId().toString() : null)
                .claim("role", target.getRole())
                .claim("tenantId", target.getTenantId() != null ? target.getTenantId().toString() : null)
                .claim("tenantSlug", target.getTenantSlug())
                .claim("isImpersonating", true)
                .claim("originalRole", originalRole)
                .claim("originalUserId", originalUserId != null ? originalUserId.toString() : null)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}