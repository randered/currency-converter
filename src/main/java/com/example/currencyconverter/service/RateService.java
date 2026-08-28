package com.example.currencyconverter.service;

import com.example.currencyconverter.dto.RateResponse;
import com.example.currencyconverter.rate.FxRateProvider;
import com.example.currencyconverter.util.Constants;
import com.example.currencyconverter.util.InputUtils;
import com.example.currencyconverter.util.LogMessages;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RateService {

    private static final Logger log = LoggerFactory.getLogger(RateService.class);

    FxRateProvider provider;

    @Cacheable(cacheNames = Constants.RATES_CACHE, key = Constants.RATES_CACHE_KEY)
    public RateResponse getRate(String sourceCurrency, String targetCurrency) {
        String normalizedSource = InputUtils.normalize(sourceCurrency);
        String normalizedTarget = InputUtils.normalize(targetCurrency);
        var rate = provider.fetchRate(normalizedSource, normalizedTarget);
        log.info(LogMessages.RATE_FETCH, normalizedSource, normalizedTarget, rate);
        return new RateResponse(normalizedSource, normalizedTarget, rate, Instant.now());
    }
}
