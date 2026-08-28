package com.example.currencyconverter.exception;

import com.example.currencyconverter.common.ErrorCode;

/** 404 — the requested client, balance, or rate does not exist. */
public class NotFoundException extends ApiException {

    public NotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
