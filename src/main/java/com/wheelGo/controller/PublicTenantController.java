package com.wheelGo.controller;

import com.wheelGo.repository.TenantRepository;
import com.wheelGo.security.ReservedTenantSlugs;
import com.wheelGo.schema.TenantPublicResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "http://localhost:5173")
@AllArgsConstructor
public class PublicTenantController {

    private final TenantRepository tenantRepository;

    @GetMapping("/tenants/{slug}")
    public ResponseEntity<?> getBySlug(@PathVariable String slug) {
        if (ReservedTenantSlugs.isReserved(slug)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return tenantRepository.findBySlug(slug)
                .map(t -> ResponseEntity.ok(
                        new TenantPublicResponse(t.getId(), t.getName(), t.getSlug())))
                .orElse(ResponseEntity.notFound().build());
    }
}
