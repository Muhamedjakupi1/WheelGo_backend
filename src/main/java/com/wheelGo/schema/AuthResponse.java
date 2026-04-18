package com.wheelGo.schema;

import java.util.UUID;

public record AuthResponse(
        String token,
        String email,
        String role,
        UUID userId,
        UUID tenantId,
        String tenantSlug,
        boolean isImpersonating,
        String originalRole,
        UUID originalUserId
) {}