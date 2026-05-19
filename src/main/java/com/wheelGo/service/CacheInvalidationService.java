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

    public void evictBookings(UUID userId) {
        Cache cache = requireCache(CacheNames.BOOKINGS);
        if (userId != null) {
            cache.evict("user:" + userId);
        }
        cache.evict("admin:all");
    }

    public void evictBookingsForUser(UUID userId) {
        requireCache(CacheNames.BOOKINGS).evict("user:" + userId);
    }

    public void evictBookingsForAdmin() {
        requireCache(CacheNames.BOOKINGS).evict("admin:all");
    }

    public void evictVehicle(UUID vehicleId) {
        Cache cache = requireCache(CacheNames.VEHICLES);
        if (vehicleId != null) {
            cache.evict("byId:" + vehicleId);
        }
        cache.evict("all");
    }

    private Cache requireCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            throw new IllegalStateException("Cache '" + cacheName + "' not found");
        }
        return cache;
    }
}
