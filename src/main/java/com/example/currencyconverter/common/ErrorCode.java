package com.example.currencyconverter.common;

import org.springframework.http.HttpStatus;

/**
 * Machine-readable error codes returned in the error body.
 * Each maps to an HTTP status so clients can branch on the code, not the text.
 */
public enum ErrorCode {

    INSUFFICIENT_FUNDS(HttpStatus.UNPROCESSABLE_ENTITY),
    CONVERSION_IN_PROGRESS(HttpStatus.CONFLICT),
    CLIENT_NOT_FOUND(HttpStatus.NOT_FOUND),
    BALANCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    RATE_NOT_FOUND(HttpStatus.NOT_FOUND),
    PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
