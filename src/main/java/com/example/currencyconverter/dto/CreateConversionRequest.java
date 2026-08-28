package com.example.currencyconverter.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Body of POST /conversions. The client identity is carried by the
 * X-Client-Id header (documented choice) and the optional Idempotency-Key
 * header, so neither is part of the body.
 */
public record CreateConversionRequest(
        @NotBlank(message = "sourceCurrency is required")
        @Pattern(regexp = "[A-Za-z]{3}", message = "sourceCurrency must be a 3-letter ISO-4217 code")
        String sourceCurrency,

        @NotBlank(message = "targetCurrency is required")
        @Pattern(regexp = "[A-Za-z]{3}", message = "targetCurrency must be a 3-letter ISO-4217 code")
        String targetCurrency,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be positive")
        @DecimalMax(value = "1000000000", message = "amount is above the allowed limit")
        @Digits(integer = 10, fraction = 2, message = "amount must have at most 2 decimal places")
        BigDecimal amount
) {
}
