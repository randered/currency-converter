package com.example.currencyconverter.service;

import com.example.currencyconverter.common.ClientLockManager;
import com.example.currencyconverter.config.ConversionLockConfig;
import com.example.currencyconverter.dto.BalanceDto;
import com.example.currencyconverter.dto.ConversionDto;
import com.example.currencyconverter.dto.ConversionResponse;
import com.example.currencyconverter.dto.CreateConversionRequest;
import com.example.currencyconverter.entity.ConversionRecord;
import com.example.currencyconverter.exception.ConflictException;
import com.example.currencyconverter.exception.ValidationException;
import com.example.currencyconverter.repository.ConversionRepository;
import com.example.currencyconverter.util.Constants;
import com.example.currencyconverter.util.InputUtils;
import com.example.currencyconverter.util.LogMessages;
import com.example.currencyconverter.util.MoneyUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversionService {

    private static final Logger log = LoggerFactory.getLogger(ConversionService.class);

    ConversionRepository conversionRepository;
    BalanceService balanceService;
    ClientLockManager lockManager;
    ConversionLockConfig lockProps;
    ConversionTransactionService conversionTransactionService;

    /**
     * The client lock is held across the whole transaction: the DB commit happens
     * inside {@link ConversionTransactionService} before the lock is released, so
     * a concurrent request never sees a half-applied conversion. The idempotency
     * re-check inside the transaction makes a duplicate insert impossible.
     */
    public ConversionResponse convert(String clientId, String idempotencyKey, CreateConversionRequest request) {
        String sourceCurrency = InputUtils.normalize(request.sourceCurrency());
        String targetCurrency = InputUtils.normalize(request.targetCurrency());
        BigDecimal amount = MoneyUtils.toBalanceScale(request.amount());

        log.info(LogMessages.CONVERT_START, clientId, sourceCurrency, targetCurrency, amount);

        // Fast-path idempotency replay (common case, no locking needed).
        Optional<ConversionRecord> replay = findExistingConversion(clientId, idempotencyKey);
        if (replay.isPresent()) {
            log.info(LogMessages.CONVERT_REPLAY, clientId, idempotencyKey, replay.get().getTransactionId());
            return toConversionResponse(replay.get(), balanceService.getBalances(clientId));
        }

        Lock lock = lockManager.lockFor(clientId);
        boolean acquired;
        if (lockProps.failFastIfInProgress()) {
            acquired = lock.tryLock();
        } else {
            lock.lock();
            acquired = true;
        }
        if (!acquired) {
            log.info(LogMessages.CONVERT_IN_PROGRESS, clientId);
            throw new ConflictException(Constants.CONVERSION_IN_PROGRESS);
        }
        try {
            ConversionRecord record = conversionTransactionService.executeConversion(
                    clientId, idempotencyKey, sourceCurrency, targetCurrency, amount);
            log.info(LogMessages.CONVERT_DONE, clientId, record.getTransactionId());
            return toConversionResponse(record, balanceService.getBalances(clientId));
        } finally {
            lock.unlock();
        }
    }

    @Transactional(readOnly = true)
    public Page<ConversionDto> getHistory(String transactionId, LocalDate date, String clientId, Pageable pageable) {
        if (transactionId == null && date == null && clientId == null) {
            throw new ValidationException(Constants.VALIDATION_FILTER_REQUIRED);
        }
        final UUID parsedTransactionId = InputUtils.parseTransactionId(transactionId);
        final Instant rangeStart = date != null ? date.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        final Instant rangeEnd = date != null ? date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;

        List<org.springframework.data.jpa.domain.Specification<ConversionRecord>> predicates =
                new java.util.ArrayList<>();
        if (clientId != null) {
            predicates.add((root, query, cb) -> cb.equal(root.get("clientId"), clientId));
        }
        if (parsedTransactionId != null) {
            predicates.add((root, query, cb) -> cb.equal(root.get("transactionId"), parsedTransactionId));
        }
        if (rangeStart != null) {
            predicates.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), rangeStart));
        }
        if (rangeEnd != null) {
            predicates.add((root, query, cb) -> cb.lessThan(root.get("createdAt"), rangeEnd));
        }

        var spec = org.springframework.data.jpa.domain.Specification.allOf(predicates);
        Page<ConversionDto> page = conversionRepository.findAll(spec, pageable)
                .map(this::toConversionDto);
        log.info(LogMessages.HISTORY, clientId, transactionId, date, page.getTotalElements());
        return page;
    }

    private Optional<ConversionRecord> findExistingConversion(String clientId, String idempotencyKey) {
        if (idempotencyKey == null) {
            return Optional.empty();
        }
        return conversionRepository.findByClientIdAndIdempotencyKey(clientId, idempotencyKey);
    }

    private ConversionResponse toConversionResponse(ConversionRecord conversion, List<BalanceDto> balances) {
        return new ConversionResponse(conversion.getTransactionId(), conversion.getSourceAmount(),
                conversion.getSourceCurrency(), conversion.getTargetAmount(), conversion.getTargetCurrency(),
                conversion.getRate(), conversion.getCreatedAt(), balances);
    }

    private ConversionDto toConversionDto(ConversionRecord conversion) {
        return new ConversionDto(conversion.getTransactionId(), conversion.getClientId(),
                conversion.getSourceAmount(), conversion.getSourceCurrency(),
                conversion.getTargetAmount(), conversion.getTargetCurrency(),
                conversion.getRate(), conversion.getCreatedAt());
    }
}
