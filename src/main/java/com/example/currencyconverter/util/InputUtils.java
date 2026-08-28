package com.example.currencyconverter.util;

import com.example.currencyconverter.exception.ValidationException;

import java.util.Locale;
import java.util.UUID;

public final class InputUtils {

    private InputUtils() {
    }

    public static String normalize(String code) {
        return code.toUpperCase(Locale.ROOT);
    }

    public static String trimToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    public static UUID parseTransactionId(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(transactionId);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(Constants.VALIDATION_TRANSACTION_ID);
        }
    }
}
