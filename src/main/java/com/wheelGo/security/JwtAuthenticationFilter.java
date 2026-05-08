package com.wheelGo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wheelGo.model.tenant.Tenant;
import com.wheelGo.model.user.User;
import com.wheelGo.repository.TenantRepository;
import com.wheelGo.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@AllArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

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
            writeUnauthorized(response, "Invalid or expired token");
            return;
        }

        Claims claims = jwtUtils.extractAllClaims(token);

        String email = claims.getSubject();
        String role = claims.get("role", String.class);
        String tenantSlug = claims.get("tenantSlug", String.class);
        UUID userId = parseUuidClaim(claims, "userId");
        UUID tenantId = parseUuidClaim(claims, "tenantId");

        Boolean isImpersonating = claims.get("isImpersonating", Boolean.class);
        String originalRole = claims.get("originalRole", String.class);
        UUID originalUserId = parseUuidClaim(claims, "originalUserId");

        if (email == null || role == null || userId == null || tenantId == null || tenantSlug == null) {
            writeUnauthorized(response, "Token is missing required claims");
            return;
        }

        Optional<User> userOptional = userRepository.findByIdAndTenantId(userId, tenantId);
        Optional<Tenant> tenantOptional = tenantRepository.findById(tenantId);

        if (userOptional.isEmpty() || tenantOptional.isEmpty()) {
            writeUnauthorized(response, "Authenticated user no longer exists");
            return;
        }

        User user = userOptional.get();
        Tenant tenant = tenantOptional.get();

        if (!user.isActive() || !tenant.isActive()) {
            writeUnauthorized(response, "Authenticated account is inactive");
            return;
        }

        if (!email.equalsIgnoreCase(user.getEmail())
                || !role.equals(user.getRole().name())
                || !tenantSlug.equals(tenant.getSlug())) {
            writeUnauthorized(response, "Token no longer matches the current account state");
            return;
        }

        CustomUserPrincipal principal = new CustomUserPrincipal(
                user.getId(),
                user.getEmail(),
                null,
                user.getRole().name(),
                tenant.getId(),
                tenant.getSlug(),
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

    private UUID parseUuidClaim(Claims claims, String claimName) {
        String value = claims.get(claimName, String.class);
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        SecurityContextHolder.clearContext();
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                message,
                null
        ));
    }
}
