package com.example.currencyconverter.exception;

import com.example.currencyconverter.common.ErrorCode;

/** 422 — the source balance cannot cover the requested amount. */
public class InsufficientFundsException extends ApiException {

    public InsufficientFundsException(String message) {
        super(ErrorCode.INSUFFICIENT_FUNDS, message);
    }
}
