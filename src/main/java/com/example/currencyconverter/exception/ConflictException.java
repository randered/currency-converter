package com.example.currencyconverter.exception;

import com.example.currencyconverter.common.ErrorCode;

/** 409 — the request conflicts with the current state, e.g. a conversion in progress. */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(ErrorCode.CONVERSION_IN_PROGRESS, message);
    }
}
