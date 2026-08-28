package com.example.currencyconverter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fx.provider")
public record FxProviderConfig(
        String baseUrl,
        int connectTimeoutMs,
        int readTimeoutMs
) {
}
