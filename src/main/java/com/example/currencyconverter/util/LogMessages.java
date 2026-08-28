package com.example.currencyconverter.util;

/** SLF4J log message templates. Arguments use the {} placeholder. */
public final class LogMessages {

    public static final String REQUEST_ENTRY = "--> {} {} clientId={}";
    public static final String REQUEST_EXIT = "<-- {} {} -> {} ({} ms)";

    public static final String CONVERT_START = "convert client={} {}->{} amount={}";
    public static final String CONVERT_REPLAY = "convert client={} idempotencyKey={} replay tx={}";
    public static final String CONVERT_IN_PROGRESS = "convert client={} conversion already in progress -> 409";
    public static final String CONVERT_DONE = "convert client={} tx={} done";
    public static final String HISTORY = "getHistory client={} tx={} date={} -> {} results";

    public static final String EXECUTE_REPLAY = "executeConversion client={} replayed tx={}";
    public static final String EXECUTE_SOURCE = "executeConversion client={} source {}={}";
    public static final String EXECUTE_RATE = "executeConversion client={} rate {}->{}={}";
    public static final String EXECUTE_DEBIT_CREDIT =
            "executeConversion client={} debit {} amount={} -> credit {} amount={}";
    public static final String EXECUTE_PERSISTED = "executeConversion client={} persisted tx={}";

    public static final String BALANCES = "getBalances client={} -> {} balances";

    public static final String RATE_FETCH = "getRate {}->{} = {}";

    private LogMessages() {
    }
}
