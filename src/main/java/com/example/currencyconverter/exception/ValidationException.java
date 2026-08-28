package com.example.currencyconverter.exception;

import com.example.currencyconverter.common.ErrorCode;

/** 400 — the request failed validation. */
public class ValidationException extends ApiException {

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}
