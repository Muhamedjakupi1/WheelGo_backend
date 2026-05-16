package com.wheelGo.model.tenant_settings;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SupportedCurrencyResponse {
    private final String code;
    private final String name;
    private final String symbol;
}
