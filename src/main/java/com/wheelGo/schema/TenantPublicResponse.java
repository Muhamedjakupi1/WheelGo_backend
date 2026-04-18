package com.wheelGo.schema;

import java.util.UUID;

public record TenantPublicResponse(
        UUID id,
        String name,
        String slug
) {}