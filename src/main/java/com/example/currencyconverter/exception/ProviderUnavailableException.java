package com.example.currencyconverter.exception;

import com.example.currencyconverter.common.ErrorCode;

/** 503 — the external FX provider could not be reached or returned garbage. */
public class ProviderUnavailableException extends ApiException {

    public ProviderUnavailableException(String message) {
        super(ErrorCode.PROVIDER_UNAVAILABLE, message);
    }

    public ProviderUnavailableException(String message, Throwable cause) {
        super(ErrorCode.PROVIDER_UNAVAILABLE, message, cause);
    }
}
