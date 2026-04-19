package com.wheelGo.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@AllArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtUtils.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims = jwtUtils.extractAllClaims(token);

        String email = claims.getSubject();
        String role = claims.get("role", String.class);
        String tenantSlug = claims.get("tenantSlug", String.class);

        UUID userId = claims.get("userId") != null
                ? UUID.fromString(claims.get("userId", String.class))
                : null;

        UUID tenantId = claims.get("tenantId") != null
                ? UUID.fromString(claims.get("tenantId", String.class))
                : null;

        Boolean isImpersonating = claims.get("isImpersonating", Boolean.class);
        String originalRole = claims.get("originalRole", String.class);

        UUID originalUserId = claims.get("originalUserId") != null
                ? UUID.fromString(claims.get("originalUserId", String.class))
                : null;

        CustomUserPrincipal principal = new CustomUserPrincipal(
                userId,
                email,
                null,
                role,
                tenantId,
                tenantSlug,
                Boolean.TRUE.equals(isImpersonating),
                originalRole,
                originalUserId
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}