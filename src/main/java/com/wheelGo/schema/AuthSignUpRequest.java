package com.wheelGo.schema;

public record AuthSignUpRequest(String email, String password, String tenantSlug) {}