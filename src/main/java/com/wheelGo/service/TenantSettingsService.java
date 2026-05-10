package com.wheelGo.service;

import com.wheelGo.model.tenant_settings.TenantSettingsRequest;
import com.wheelGo.model.tenant_settings.TenantSettingsResponse;
import com.wheelGo.model.tenant_settings.TenantSettingsUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantSettingsService {

    private static final String DEFAULT_CURRENCY = "EUR";
    private static final String DEFAULT_TIMEZONE = "UTC";
    private static final String DEFAULT_THEME_COLOR = "#1A73E8";

    private final JdbcTemplate jdbcTemplate;

    public TenantSettingsResponse getForTenant(String schemaName) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id, currency, timezone, logo_url, theme_color " +
                            "FROM " + qualify(schemaName, "tenant_settings") + " " +
                            "ORDER BY created_at ASC LIMIT 1",
                    (rs, rowNum) -> mapResponse(rs)
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public TenantSettingsResponse createForTenant(String schemaName, TenantSettingsRequest request) {
        String currency = normalizeValue(request != null ? request.getCurrency() : null, DEFAULT_CURRENCY);
        String timezone = normalizeValue(request != null ? request.getTimezone() : null, DEFAULT_TIMEZONE);
        String logoUrl = normalizeOptional(request != null ? request.getLogoUrl() : null);
        String themeColor = normalizeValue(request != null ? request.getThemeColor() : null, DEFAULT_THEME_COLOR);

        return jdbcTemplate.queryForObject(
                "INSERT INTO " + qualify(schemaName, "tenant_settings") + " " +
                        "(currency, timezone, logo_url, theme_color, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, NOW(), NOW()) " +
                        "RETURNING id, currency, timezone, logo_url, theme_color",
                (rs, rowNum) -> mapResponse(rs),
                currency, timezone, logoUrl, themeColor
        );
    }

    public TenantSettingsResponse updateForTenant(String schemaName, TenantSettingsUpdateRequest request) {
        TenantSettingsResponse current = getForTenant(schemaName);
        if (current == null) {
            TenantSettingsRequest createRequest = new TenantSettingsRequest();
            if (request != null) {
                createRequest.setCurrency(request.getCurrency());
                createRequest.setTimezone(request.getTimezone());
                createRequest.setLogoUrl(request.getLogoUrl());
                createRequest.setThemeColor(request.getThemeColor());
            }
            return createForTenant(schemaName, createRequest);
        }

        String currency = normalizeValue(request != null ? request.getCurrency() : current.getCurrency(), DEFAULT_CURRENCY);
        String timezone = normalizeValue(request != null ? request.getTimezone() : current.getTimezone(), DEFAULT_TIMEZONE);
        String logoUrl = request != null && request.getLogoUrl() != null
                ? normalizeOptional(request.getLogoUrl())
                : current.getLogoUrl();
        String themeColor = normalizeValue(request != null ? request.getThemeColor() : current.getThemeColor(), DEFAULT_THEME_COLOR);

        return jdbcTemplate.queryForObject(
                "UPDATE " + qualify(schemaName, "tenant_settings") + " " +
                        "SET currency = ?, timezone = ?, logo_url = ?, theme_color = ?, updated_at = NOW() " +
                        "WHERE id = ? " +
                        "RETURNING id, currency, timezone, logo_url, theme_color",
                (rs, rowNum) -> mapResponse(rs),
                currency, timezone, logoUrl, themeColor, current.getId()
        );
    }

    private TenantSettingsResponse mapResponse(ResultSet rs) throws SQLException {
        TenantSettingsResponse response = new TenantSettingsResponse();
        response.setId(rs.getObject("id", UUID.class));
        response.setCurrency(rs.getString("currency"));
        response.setTimezone(rs.getString("timezone"));
        response.setLogoUrl(rs.getString("logo_url"));
        response.setThemeColor(rs.getString("theme_color"));
        return response;
    }

    private String qualify(String schemaName, String tableName) {
        String schema = Objects.requireNonNull(schemaName, "schemaName must not be null").trim();
        if (!schema.matches("^[a-z][a-z0-9_]{0,62}$")) {
            throw new IllegalArgumentException("Invalid tenant schema name.");
        }
        return "\"" + schema + "\"." + tableName;
    }

    private String normalizeValue(String value, String fallback) {
        String normalized = normalizeOptional(value);
        return normalized == null ? fallback : normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
