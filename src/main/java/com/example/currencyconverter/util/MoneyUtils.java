package com.example.currencyconverter.util;

import com.example.currencyconverter.entity.Balance;
import com.example.currencyconverter.exception.InsufficientFundsException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {

    public static final int BALANCE_SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    private MoneyUtils() {
    }

    public static BigDecimal toBalanceScale(BigDecimal value) {
        return value.setScale(BALANCE_SCALE, ROUNDING);
    }

    public static BigDecimal convert(BigDecimal sourceAmount, BigDecimal rate) {
        return sourceAmount.multiply(rate).setScale(BALANCE_SCALE, ROUNDING);
    }

    public static void checkSufficientFunds(Balance balance, BigDecimal sourceAmount) {
        if (balance.getAmount().compareTo(sourceAmount) < 0) {
            throw new InsufficientFundsException(Constants.INSUFFICIENT_FUNDS);
        }
    }
}
