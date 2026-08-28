package com.example.currencyconverter.dto;

import java.math.BigDecimal;

public record BalanceDto(
        String currency,
        BigDecimal amount
) {
}
