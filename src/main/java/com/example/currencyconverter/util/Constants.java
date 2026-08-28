package com.example.currencyconverter.util;

/**
 * Shared constants used across the application. Keep magic strings out of
 * business and infrastructure code. Message templates use {@code %s} and are
 * substituted with {@link String#formatted(Object...)}.
 */
public final class Constants {

    /** FX provider endpoint template; {base} is replaced with the base currency. */
    public static final String FX_PROVIDER_LATEST_PATH = "/latest/{base}";

    /** Success marker returned by the FX provider. */
    public static final String FX_PROVIDER_RESULT_SUCCESS = "success";

    public static final String FX_PROVIDER_UNREACHABLE = "Unable to reach FX provider";
    public static final String FX_PROVIDER_EMPTY_RESPONSE = "FX provider returned an empty response";
    public static final String FX_PROVIDER_HTTP_ERROR_PREFIX = "FX provider returned HTTP ";

    /** Header carrying the caller-supplied client identity. */
    public static final String HEADER_CLIENT_ID = "X-Client-Id";

    /** Optional header that makes POST /conversions idempotent. */
    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";

    public static final String HEADER_CLIENT_ID_REQUIRED = HEADER_CLIENT_ID + " must not be blank";
    public static final String HEADER_IDEMPOTENCY_KEY_INVALID =
            HEADER_IDEMPOTENCY_KEY + " must not be blank and must be at most 128 characters";

    /** Caffeine cache name for FX rates. */
    public static final String RATES_CACHE = "rates";

    /** Cache key built from the currency pair (SpEL for @Cacheable). */
    public static final String RATES_CACHE_KEY = "#sourceCurrency + ':' + #targetCurrency";

    public static final String VALIDATION_FILTER_REQUIRED =
            "At least one filter must be provided: transactionId, date, or clientId";
    public static final String VALIDATION_TRANSACTION_ID = "transactionId must be a valid UUID";
    public static final String VALIDATION_FAILED = "Request validation failed";
    public static final String MALFORMED_REQUEST = "Malformed request";
    public static final String UNEXPECTED_ERROR = "Unexpected server error";

    public static final String INSUFFICIENT_FUNDS = "Insufficient funds in source currency balance";

    public static final String CONVERSION_IN_PROGRESS =
            "A conversion for this client is already in progress. Please retry shortly.";

    /** %s = client id */
    public static final String CLIENT_NOT_FOUND = "Client not found: %s";

    /** %s = client id, %s = currency */
    public static final String BALANCE_NOT_FOUND = "Client %s has no balance in currency %s";

    /** %s = currency */
    public static final String RATE_NOT_FOUND = "No exchange rate available for currency %s";

    private Constants() {
    }
}
