package com.wheelGo.service;

import com.wheelGo.mapper.TenantMapper;
import com.wheelGo.model.enums.Role;
import com.wheelGo.model.tenant.TenantRequest;
import com.wheelGo.model.tenant.Tenant;
import com.wheelGo.model.tenant.TenantResponse;
import com.wheelGo.model.tenant.TenantUpdateRequest;
import com.wheelGo.model.user.User;
import com.wheelGo.repository.TenantRepository;
import com.wheelGo.repository.UserRepository;
import com.wheelGo.schema.TenantSchemaService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantSchemaService schemaService;
    private final TenantMapper tenantMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TenantResponse createTenant(TenantRequest req) {
        String normalizedSlug = normalizeSlug(req.getSlug());

        if (normalizedSlug == null || normalizedSlug.isBlank()) {
            throw new RuntimeException("Slug is required.");
        }

        if (tenantRepository.existsBySlug(normalizedSlug)) {
            throw new RuntimeException("Slug '" + normalizedSlug + "' already exists.");
        }

        if (userRepository.existsByEmail(req.getAdminEmail().trim().toLowerCase())) {
            throw new RuntimeException("Admin email already exists.");
        }

        String schemaName = normalizedSlug.replace("-", "_");

        Tenant tenant = new Tenant();
        tenant.setName(req.getName().trim());
        tenant.setSlug(normalizedSlug);
        tenant.setSchemaName(schemaName);
        tenant.setPlan(req.getPlan());

        Tenant savedTenant = tenantRepository.save(tenant);
        schemaService.createSchemaForTenant(schemaName);

        User adminUser = new User();
        adminUser.setEmail(req.getAdminEmail().trim().toLowerCase());
        adminUser.setPasswordHash(passwordEncoder.encode(req.getAdminPassword()));
        adminUser.setRole(Role.ADMIN);
        adminUser.setTenant(savedTenant);
        adminUser.setActive(true);
        adminUser.setEmailVerified(true);

        userRepository.save(adminUser);

        log.info("Tenant '{}' was created with schema '{}' and admin '{}'.",
                savedTenant.getSlug(), schemaName, adminUser.getEmail());

        return tenantMapper.toResponse(savedTenant);
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
    public TenantResponse updateTenant(UUID id, TenantUpdateRequest req) {
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
        Tenant updated = tenantRepository.save(tenant);
        return tenantMapper.toResponse(updated);
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