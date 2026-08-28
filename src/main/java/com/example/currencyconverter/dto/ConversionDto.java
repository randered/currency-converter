package com.example.currencyconverter.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ConversionDto(
        UUID transactionId,
        String clientId,
        BigDecimal sourceAmount,
        String sourceCurrency,
        BigDecimal targetAmount,
        String targetCurrency,
        BigDecimal rate,
        Instant timestamp
) {
}
