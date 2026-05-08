package com.wheelGo.controller;

import com.wheelGo.auth.AuthLoginRequest;
import com.wheelGo.auth.AuthSignUpRequest;
import com.wheelGo.security.ApiErrorResponse;
import com.wheelGo.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login/{tenantSlug}")
    public ResponseEntity<?> login(@PathVariable String tenantSlug, @RequestBody AuthLoginRequest req) {
        try {
           AuthLoginRequest request = new AuthLoginRequest(
                   req.email(),
                   req.password(),
                   tenantSlug
           );
           return ResponseEntity.ok(authService.login(request));
        } catch (ResponseStatusException e) {
            return buildErrorResponse(HttpStatus.valueOf(e.getStatusCode().value()), e.getReason(), "/api/auth/login/" + tenantSlug);
        } catch (RuntimeException e) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage(), "/api/auth/login/" + tenantSlug);
        }
    }

    @PostMapping("/signup/{tenantSlug}")
    public ResponseEntity<?> signup(@PathVariable String tenantSlug,
                                    @RequestBody AuthSignUpRequest req) {
        try {
            AuthSignUpRequest request = new AuthSignUpRequest(
                    req.email(),
                    req.password(),
                    tenantSlug
            );
            return ResponseEntity.ok(authService.signup(request));
        } catch (ResponseStatusException e) {
            return buildErrorResponse(HttpStatus.valueOf(e.getStatusCode().value()), e.getReason(), "/api/auth/signup/" + tenantSlug);
        } catch (RuntimeException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), "/api/auth/signup/" + tenantSlug);
        }
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(HttpStatus status, String message, String path) {
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        path
                ));
    }
}
