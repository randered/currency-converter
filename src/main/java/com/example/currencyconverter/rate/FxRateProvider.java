package com.example.currencyconverter.rate;

import java.math.BigDecimal;

/**
 * Abstraction over the external FX rate source so the rest of the app and the
 * tests never talk to a specific vendor.
 */
public interface FxRateProvider {

    BigDecimal fetchRate(String from, String to);
}
