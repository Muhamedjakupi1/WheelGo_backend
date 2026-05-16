package com.wheelGo.auth;

public record AuthSignUpRequest(
        String email,
        String password,
        String tenantSlug,
        String firstName,
        String lastName,
        String phone
) {}
