package com.wheelGo.service;

import com.wheelGo.model.tenant.CreateTenantRequest;
import com.wheelGo.model.tenant.Tenant;
import com.wheelGo.model.tenant.UpdateTenantRequest;
import com.wheelGo.repository.TenantRepository;
import com.wheelGo.schema.TenantSchemaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class TenantService {

    private final TenantRepository    tenantRepository;
    private final TenantSchemaService schemaService;

    public TenantService(TenantRepository tenantRepository,
                         TenantSchemaService schemaService) {
        this.tenantRepository = tenantRepository;
        this.schemaService    = schemaService;
    }

    @Transactional
    public Tenant createTenant(CreateTenantRequest req) {
        if (tenantRepository.existsBySlug(req.getSlug())) {
            throw new RuntimeException(
                    "Slug '" + req.getSlug() + "' ekziston tashmë."
            );
        }

        String schemaName = req.getSlug()
                .toLowerCase()
                .replace("-", "_");

        Tenant tenant = new Tenant();
        tenant.setName(req.getName());
        tenant.setSlug(req.getSlug());
        tenant.setSchemaName(schemaName);
        tenant.setPlan(req.getPlan());

        Tenant saved = tenantRepository.save(tenant);
        schemaService.createSchemaForTenant(schemaName);

        log.info("Tenant '{}' u krijua me schemën '{}'.",
                saved.getSlug(), schemaName);
        return saved;
    }

    public List<Tenant> getAll() {
        return tenantRepository.findAll();
    }
    @Transactional
    public void deleteTenant(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant nuk u gjet."));

        schemaService.dropSchemaForTenant(tenant.getSchemaName());

        tenantRepository.delete(tenant);

        log.info("Tenant '{}' dhe schema '{}' u fshinë.",
                tenant.getSlug(), tenant.getSchemaName());
    }
    @Transactional
    public Tenant updateTenant(UUID id, UpdateTenantRequest req) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant nuk u gjet."));

        if (req.getName() != null) {
            tenant.setName(req.getName());
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
}