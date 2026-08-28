package com.example.currencyconverter.exception;

import com.example.currencyconverter.common.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Base exception for all business/API errors. Handlers map it to the status
 * carried by the {@link ErrorCode}.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ApiException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return errorCode.status();
    }
}
