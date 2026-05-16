package com.wheelGo.service;

import com.wheelGo.model.tenant_settings.SupportedCurrencyResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SupportedCurrencyService {

    private static final Map<String, SupportedCurrencyResponse> SUPPORTED_CURRENCIES = Map.ofEntries(
            Map.entry("USD", new SupportedCurrencyResponse("USD", "US Dollar", "$")),
            Map.entry("EUR", new SupportedCurrencyResponse("EUR", "Euro", "EUR")),
            Map.entry("GBP", new SupportedCurrencyResponse("GBP", "British Pound", "£")),
            Map.entry("JPY", new SupportedCurrencyResponse("JPY", "Japanese Yen", "JPY")),
            Map.entry("CHF", new SupportedCurrencyResponse("CHF", "Swiss Franc", "CHF")),
            Map.entry("CAD", new SupportedCurrencyResponse("CAD", "Canadian Dollar", "C$")),
            Map.entry("AUD", new SupportedCurrencyResponse("AUD", "Australian Dollar", "A$")),
            Map.entry("PLN", new SupportedCurrencyResponse("PLN", "Polish Zloty", "zl")),
            Map.entry("CNY", new SupportedCurrencyResponse("CNY", "Chinese Yuan", "CNY")),
            Map.entry("AED", new SupportedCurrencyResponse("AED", "UAE Dirham", "AED"))
    );

    public List<SupportedCurrencyResponse> getSupportedCurrencies() {
        return List.of(
                SUPPORTED_CURRENCIES.get("USD"),
                SUPPORTED_CURRENCIES.get("EUR"),
                SUPPORTED_CURRENCIES.get("GBP"),
                SUPPORTED_CURRENCIES.get("JPY"),
                SUPPORTED_CURRENCIES.get("CHF"),
                SUPPORTED_CURRENCIES.get("CAD"),
                SUPPORTED_CURRENCIES.get("AUD"),
                SUPPORTED_CURRENCIES.get("PLN"),
                SUPPORTED_CURRENCIES.get("CNY"),
                SUPPORTED_CURRENCIES.get("AED")
        );
    }

    public String normalizeAndValidate(String currencyCode) {
        String normalized = currencyCode == null ? null : currencyCode.trim().toUpperCase(Locale.ROOT);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        if (!SUPPORTED_CURRENCIES.containsKey(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported currency: " + normalized);
        }
        return normalized;
    }

    public SupportedCurrencyResponse getByCode(String currencyCode) {
        if (currencyCode == null) {
            return null;
        }
        String normalized = currencyCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return null;
        }
        return SUPPORTED_CURRENCIES.get(normalized);
    }
}
