package com.example.currencyconverter.service;

import com.example.currencyconverter.common.ErrorCode;
import com.example.currencyconverter.entity.Balance;
import com.example.currencyconverter.entity.ConversionRecord;
import com.example.currencyconverter.exception.NotFoundException;
import com.example.currencyconverter.repository.BalanceRepository;
import com.example.currencyconverter.repository.ClientRepository;
import com.example.currencyconverter.repository.ConversionRepository;
import com.example.currencyconverter.util.Constants;
import com.example.currencyconverter.util.LogMessages;
import com.example.currencyconverter.util.MoneyUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Runs a single conversion inside its own transaction. {@link ConversionService}
 * holds the client lock while calling this, so the commit completes before the
 * lock is released — a concurrent request never sees a half-applied conversion.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversionTransactionService {

    private static final Logger log = LoggerFactory.getLogger(ConversionTransactionService.class);

    ClientRepository clientRepository;
    BalanceRepository balanceRepository;
    ConversionRepository conversionRepository;
    RateService rateService;

    /**
     * Returns the persisted (or replayed) conversion record. Called while the
     * client lock is held, so the idempotency re-check sees any duplicate that
     * committed while the caller waited on the lock.
     */
    @Transactional
    public ConversionRecord executeConversion(String clientId, String idempotencyKey,
                                              String sourceCurrency, String targetCurrency, BigDecimal amount) {
        Optional<ConversionRecord> replay = findExistingConversion(clientId, idempotencyKey);
        if (replay.isPresent()) {
            log.info(LogMessages.EXECUTE_REPLAY, clientId, replay.get().getTransactionId());
            return replay.get();
        }

        clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CLIENT_NOT_FOUND,
                        Constants.CLIENT_NOT_FOUND.formatted(clientId)));

        Balance source = balanceRepository.findByClientIdAndCurrency(clientId, sourceCurrency)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BALANCE_NOT_FOUND,
                        Constants.BALANCE_NOT_FOUND.formatted(clientId, sourceCurrency)));
        log.info(LogMessages.EXECUTE_SOURCE, clientId, sourceCurrency, source.getAmount());

        var rate = rateService.getRate(sourceCurrency, targetCurrency);
        log.info(LogMessages.EXECUTE_RATE, clientId, sourceCurrency, targetCurrency, rate.rate());

        MoneyUtils.checkSufficientFunds(source, amount);
        source.applyDelta(amount.negate());

        Balance target = balanceRepository.findByClientIdAndCurrency(clientId, targetCurrency)
                .orElseGet(() -> Balance.builder()
                        .clientId(clientId)
                        .currency(targetCurrency)
                        .amount(BigDecimal.ZERO)
                        .build());
        BigDecimal targetAmount = MoneyUtils.convert(amount, rate.rate());
        target.applyDelta(targetAmount);
        balanceRepository.save(target);
        log.info(LogMessages.EXECUTE_DEBIT_CREDIT, clientId, sourceCurrency, amount, targetCurrency, targetAmount);

        ConversionRecord record = ConversionRecord.builder()
                .transactionId(UUID.randomUUID())
                .clientId(clientId)
                .sourceCurrency(sourceCurrency)
                .targetCurrency(targetCurrency)
                .sourceAmount(amount)
                .targetAmount(targetAmount)
                .rate(rate.rate())
                .idempotencyKey(idempotencyKey)
                .build();
        conversionRepository.save(record);
        log.info(LogMessages.EXECUTE_PERSISTED, clientId, record.getTransactionId());

        return record;
    }

    private Optional<ConversionRecord> findExistingConversion(String clientId, String idempotencyKey) {
        if (idempotencyKey == null) {
            return Optional.empty();
        }
        return conversionRepository.findByClientIdAndIdempotencyKey(clientId, idempotencyKey);
    }
}
