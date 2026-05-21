package com.wheelGo.service;

import com.wheelGo.schema.TenantContext;
import org.springframework.stereotype.Service;

@Service
public class TenantCacheKeyService {

    public String currentTenantScope() {
        String schema = TenantContext.getCurrentSchema();
        return schema != null && !schema.isBlank() ? schema : "public";
    }
}
