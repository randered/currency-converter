package com.example.currencyconverter.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreateConversionRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validRequestPasses() {
        var violations = validator.validate(new CreateConversionRequest("USD", "EUR", new BigDecimal("100.00")));
        assertThat(violations).isEmpty();
    }

    @Test
    void missingSourceCurrencyFails() {
        var violations = validator.validate(new CreateConversionRequest("", "EUR", new BigDecimal("10.00")));
        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .contains("sourceCurrency");
    }

    @Test
    void nonIsoCurrencyFails() {
        var violations = validator.validate(new CreateConversionRequest("US", "EUR", new BigDecimal("10.00")));
        assertThat(violations).isNotEmpty();
    }

    @Test
    void negativeAmountFails() {
        var violations = validator.validate(new CreateConversionRequest("USD", "EUR", new BigDecimal("-5.00")));
        assertThat(violations).extracting(v -> v.getPropertyPath().toString()).contains("amount");
    }

    @Test
    void zeroAmountFails() {
        var violations = validator.validate(new CreateConversionRequest("USD", "EUR", BigDecimal.ZERO));
        assertThat(violations).isNotEmpty();
    }

    @Test
    void tooManyDecimalPlacesFails() {
        var violations = validator.validate(new CreateConversionRequest("USD", "EUR", new BigDecimal("10.001")));
        assertThat(violations).extracting(v -> v.getPropertyPath().toString()).contains("amount");
    }

    @Test
    void absurdlyLargeAmountFails() {
        var violations = validator.validate(new CreateConversionRequest("USD", "EUR", new BigDecimal("10000000000")));
        assertThat(violations).isNotEmpty();
    }
}
