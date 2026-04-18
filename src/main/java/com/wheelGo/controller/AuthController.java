package com.wheelGo.controller;

import com.wheelGo.schema.AuthLoginRequest;
import com.wheelGo.schema.AuthSignUpRequest;
import com.wheelGo.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthLoginRequest req) {
        try {
            return ResponseEntity.ok(authService.login(req));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
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
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }
}