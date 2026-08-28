package com.example.currencyconverter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Controls how concurrent conversions for the same client behave.
 * When {@code failFastIfInProgress} is true, a second request that arrives while a
 * conversion is already running for that client fails fast with a 409 instead
 * of blocking on the lock.
 */
@ConfigurationProperties(prefix = "conversions")
public record ConversionLockConfig(boolean failFastIfInProgress) {
}
