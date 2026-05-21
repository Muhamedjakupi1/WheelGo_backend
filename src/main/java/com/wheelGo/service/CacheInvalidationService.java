package com.wheelGo.service;

import com.wheelGo.config.CacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CacheInvalidationService {

    private final CacheManager cacheManager;
    private final TenantCacheKeyService tenantCacheKeyService;

    /**
     * Fshin saktësisht vetëm cache-in e rezervimeve të atij përdoruesi.
     */
    public void evictBookings(UUID userId) {
        if (userId != null) {
            Cache cache = requireCache(CacheNames.BOOKINGS);
            // Fshin saktësisht çelësin "user:UUID" brenda "bookings_v1"
            cache.evict("user:" + userId);
        }
    }

    public void evictBookingsForUser(UUID userId) {
        evictBookings(userId);
    }

    /**
     * Nëse admini shikon një listë të përgjithshme (psh. "bookings_v1::all")
     */
    public void evictBookingsForAdmin() {
        Cache cache = requireCache(CacheNames.BOOKINGS);
        cache.evict("admin:all:" + tenantCacheKeyService.currentTenantScope());
    }

    /**
     * Nëse dëshiron t'i fshish KREJT rezervimet e të gjithë përdoruesve me një herë
     */
    public void clearAllBookings() {
        requireCache(CacheNames.BOOKINGS).clear();
    }

    /**
     * Fshin një automjet specifik dhe listën e përgjithshme të automjeteve.
     */
    public void evictVehicle(UUID vehicleId) {
        Cache cache = requireCache(CacheNames.VEHICLES);
        String tenantScope = tenantCacheKeyService.currentTenantScope();
        if (vehicleId != null) {
            cache.evict("byId:" + tenantScope + ":" + vehicleId);
        }
        cache.evict("all:" + tenantScope);
    }

    public void evictUsersForTenant(UUID tenantId) {
        if (tenantId == null) {
            return;
        }
        Cache cache = requireCache(CacheNames.USERS);
        cache.evict("all:" + tenantId);
    }

    public void evictUserByIdForTenant(UUID tenantId, UUID userId) {
        if (tenantId == null || userId == null) {
            return;
        }
        Cache cache = requireCache(CacheNames.USERS);
        cache.evict("byId:" + tenantId + ":" + userId);
    }

    private Cache requireCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            throw new IllegalStateException("Cache '" + cacheName + "' nuk u gjet në Redis");
        }
        return cache;
    }
}
