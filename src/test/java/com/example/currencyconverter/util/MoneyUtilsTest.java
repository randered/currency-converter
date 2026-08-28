package com.example.currencyconverter.util;

import com.example.currencyconverter.entity.Balance;
import com.example.currencyconverter.common.ErrorCode;
import com.example.currencyconverter.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyUtilsTest {

    @Test
    void convertMultipliesAndRoundsToBalanceScale() {
        BigDecimal target = MoneyUtils.convert(new BigDecimal("100.00"), new BigDecimal("0.8542314567"));
        assertThat(target).isEqualByComparingTo("85.42"); // HALF_EVEN: 85.423... -> 85.42
    }

    @Test
    void convertUsesHalfEvenRounding() {
        // 0.005 rounds to even neighbour (0.00) under HALF_EVEN.
        assertThat(MoneyUtils.convert(new BigDecimal("1.00"), new BigDecimal("0.005")))
                .isEqualByComparingTo("0.00");
        // 0.015 rounds to 0.02 under HALF_EVEN (2 is even).
        assertThat(MoneyUtils.convert(new BigDecimal("1.00"), new BigDecimal("0.015")))
                .isEqualByComparingTo("0.02");
    }

    @Test
    void toBalanceScaleNormalizesScale() {
        BigDecimal normalized = MoneyUtils.toBalanceScale(new BigDecimal("10.1"));
        assertThat(normalized).isEqualByComparingTo("10.10");
        assertThat(normalized.scale()).isEqualTo(MoneyUtils.BALANCE_SCALE);
    }

    @Test
    void checkSufficientFundsAcceptsExactAmount() {
        Balance balance = new Balance("C1", "USD", new BigDecimal("50.00"));
        MoneyUtils.checkSufficientFunds(balance, new BigDecimal("50.00"));
    }

    @Test
    void checkSufficientFundsRejectsOverdraft() {
        Balance balance = new Balance("C1", "USD", new BigDecimal("50.00"));
        assertThatThrownBy(() -> MoneyUtils.checkSufficientFunds(balance, new BigDecimal("50.01")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);
    }
}
