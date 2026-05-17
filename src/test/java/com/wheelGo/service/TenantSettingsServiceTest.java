package com.wheelGo.service;

import com.wheelGo.model.tenant_settings.SupportedCurrencyResponse;
import com.wheelGo.model.tenant_settings.TenantSettingsRequest;
import com.wheelGo.model.tenant_settings.TenantSettingsResponse;
import com.wheelGo.model.tenant_settings.TenantSettingsUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantSettingsServiceTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private SupportedCurrencyService supportedCurrencyService;
    @InjectMocks private TenantSettingsService tenantSettingsService;

    @Test
    void should_return_null_when_no_settings_exist_for_tenant() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        TenantSettingsResponse result = tenantSettingsService.getForTenant("tenant_one");

        assertThat(result).isNull();
    }

    @Test
    void should_create_tenant_settings_with_normalized_values() {
        TenantSettingsRequest request = new TenantSettingsRequest();
        request.setCurrency(" pln ");
        request.setTimezone(" Europe/Warsaw ");
        request.setLogoUrl("  ");
        request.setThemeColor(" #ffffff ");

        TenantSettingsResponse response = new TenantSettingsResponse();
        response.setCurrency("PLN");

        when(supportedCurrencyService.normalizeAndValidate(" pln ")).thenReturn("PLN");
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("PLN"), eq("Europe/Warsaw"), eq(null), eq("#ffffff")))
                .thenReturn(response);

        TenantSettingsResponse result = tenantSettingsService.createForTenant("tenant_one", request);

        assertThat(result.getCurrency()).isEqualTo("PLN");
    }

    @Test
    void should_update_existing_settings_when_current_exists() {
        TenantSettingsResponse current = new TenantSettingsResponse();
        current.setId(UUID.randomUUID());
        current.setCurrency("EUR");
        current.setTimezone("UTC");
        current.setLogoUrl("old");
        current.setThemeColor("#123");

        TenantSettingsUpdateRequest request = new TenantSettingsUpdateRequest();
        request.setLogoUrl("  ");
        request.setThemeColor(" #456 ");

        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class))).thenReturn(current);
        when(supportedCurrencyService.normalizeAndValidate(nullable(String.class))).thenReturn(null);

        TenantSettingsResponse updated = new TenantSettingsResponse();
        updated.setThemeColor("#456");

        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("EUR"), eq("UTC"), eq(null), eq("#456"), eq(current.getId())))
                .thenReturn(updated);

        TenantSettingsResponse result = tenantSettingsService.updateForTenant("tenant_one", request);

        assertThat(result.getThemeColor()).isEqualTo("#456");
    }

    @Test
    void should_create_settings_when_updating_missing_tenant_settings() {
        TenantSettingsUpdateRequest request = new TenantSettingsUpdateRequest();
        request.setCurrency("USD");
        TenantSettingsResponse created = new TenantSettingsResponse();
        created.setCurrency("USD");

        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class))).thenThrow(new EmptyResultDataAccessException(1));
        when(supportedCurrencyService.normalizeAndValidate("USD")).thenReturn("USD");
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("USD"), eq("UTC"), eq(null), eq("#1A73E8")))
                .thenReturn(created);

        TenantSettingsResponse result = tenantSettingsService.updateForTenant("tenant_one", request);

        assertThat(result.getCurrency()).isEqualTo("USD");
    }

    @Test
    void should_throw_illegal_argument_when_schema_name_invalid() {
        assertThatThrownBy(() -> tenantSettingsService.getForTenant("bad-schema"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid tenant schema name");
    }
}
