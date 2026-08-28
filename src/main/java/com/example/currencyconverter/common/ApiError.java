package com.example.currencyconverter.common;

import java.time.Instant;
import java.util.List;

public record ApiError(String code, String message, Instant timestamp, List<FieldError> details) {

    public record FieldError(String field, String message) {
    }

    public static ApiError of(ErrorCode errorCode, String message) {
        return new ApiError(errorCode.name(), message, Instant.now(), null);
    }

    public static ApiError validation(String message, List<FieldError> details) {
        return new ApiError(ErrorCode.VALIDATION_ERROR.name(), message, Instant.now(), details);
    }
}
