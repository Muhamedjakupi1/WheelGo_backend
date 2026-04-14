package com.wheelGo.service;

import com.wheelGo.model.tenant.CreateTenantRequest;
import com.wheelGo.model.tenant.Tenant;
import com.wheelGo.model.tenant.UpdateTenantRequest;
import com.wheelGo.repository.TenantRepository;
import com.wheelGo.schema.TenantSchemaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantSchemaService schemaService;

    public TenantService(TenantRepository tenantRepository,
                         TenantSchemaService schemaService) {
        this.tenantRepository = tenantRepository;
        this.schemaService = schemaService;
    }

    @Transactional
    public Tenant createTenant(CreateTenantRequest req) {
        String normalizedSlug = normalizeSlug(req.getSlug());

        if (normalizedSlug == null || normalizedSlug.isBlank()) {
            throw new RuntimeException("Slug is required.");
        }

        if (tenantRepository.existsBySlug(normalizedSlug)) {
            throw new RuntimeException("Slug '" + normalizedSlug + "' already exists.");
        }

        String schemaName = normalizedSlug.replace("-", "_");

        Tenant tenant = new Tenant();
        tenant.setName(req.getName());
        tenant.setSlug(normalizedSlug);
        tenant.setSchemaName(schemaName);
        tenant.setPlan(req.getPlan());

        Tenant saved = tenantRepository.save(tenant);
        schemaService.createSchemaForTenant(schemaName);

        log.info("Tenant '{}' was created with schema '{}'.", saved.getSlug(), schemaName);
        return saved;
    }

    public List<Tenant> getAll() {
        return tenantRepository.findAll();
    }

    @Transactional
    public void deleteTenant(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found."));

        schemaService.dropSchemaForTenant(tenant.getSchemaName());
        tenantRepository.delete(tenant);

        log.info("Tenant '{}' and schema '{}' were deleted.",
                tenant.getSlug(), tenant.getSchemaName());
    }

    @Transactional
    public Tenant updateTenant(UUID id, UpdateTenantRequest req) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found."));

        if (req.getName() != null && !req.getName().isBlank()) {
            tenant.setName(req.getName().trim());
        }

        if (req.getPlan() != null) {
            tenant.setPlan(req.getPlan());
        }

        if (req.getIsActive() != null) {
            tenant.setActive(req.getIsActive());
        }

        tenant.setUpdatedAt(LocalDateTime.now());
        return tenantRepository.save(tenant);
    }

    private String normalizeSlug(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        normalized = normalized
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+|-+$", "");

        return normalized;
    }
}