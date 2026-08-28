package com.example.currencyconverter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.Map;

/** Wire format of GET /v6/latest/{base}. Unknown metadata fields are ignored. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenErRateResponse(String result, Map<String, BigDecimal> rates) {
}
